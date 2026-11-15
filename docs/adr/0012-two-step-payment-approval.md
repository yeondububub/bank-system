# ADR-0012: 2단계 결제 승인 프로세스 및 대사(Reconciliation) 시스템 도입

- **상태(Status)**: Accepted (Amends [ADR-0002](0002-pessimistic-lock-concurrency-control.md) & [ADR-0004](0004-account-domain-integration.md))
- **영향 범위**: `module-domain`, `module-infra`, `module-client`, `module-pg`, `module-api`

---

## 1. 배경 (Context)
초기 결제 승인(`PaymentFacade.approvePayment`) 구조는 **계좌 잔액 차감(DB)** 과 **외부 PG사 승인 API 호출(`pgPort.pay`)** 을 단일 `@Transactional` 범위 안에서 동기식으로 실행했습니다. 이 구조에는 두 가지 치명적인 문제가 있었습니다:

1. **DB 커넥션 고갈 (Connection Starvation)**:
   - 외부 PG사 API 호출은 네트워크 통신이므로 수백 ms에서 수 초의 지연이 발생할 수 있습니다.
   - 외부 응답을 기다리는 동안 DB 커넥션과 행 락(Row Lock)을 계속 점유하게 되어, 트래픽 집중 시 HikariCP 커넥션 풀이 고갈되어 전체 서비스가 마비될 위험이 큽니다.
2. **외부 PG와 내부 DB 간 상태 불일치**:
   - 외부 PG사 결제는 성공했으나 이후 로컬 DB 커밋 단계에서 네트워크 단절이나 서버 장애로 롤백되면, **고객은 PG사에서 실제 결제가 완료되었음에도 내부 시스템에는 미결제 상태로 남는 재무 불일치**가 발생합니다.

---

## 2. 결정 사항 (Decision)
외부 네트워크 호출과 로컬 DB 트랜잭션을 철저히 분리하는 **2단계 결제 승인 프로세스(2-Step Approval)** 와, 장애 시 최종 정합성(Eventual Consistency)을 복구하는 **대사(Reconciliation) 스케줄러**를 도입합니다.

### 2.1. 트랜잭션 분리 및 `PaymentTransactionService` 도입
Spring AOP 프록시 특성상 동일 클래스(`PaymentFacade`) 내부에서 메서드를 자가 호출(Self-Invocation)하면 `@Transactional`이 동작하지 않습니다. 이를 깔끔하게 해결하고 책임을 분리하기 위해 로컬 DB 트랜잭션 전담 컴포넌트인 **`PaymentTransactionService`** 를 분리했습니다.

1. **오케스트레이터 (`PaymentFacade.approvePayment`)**:
   - `@Transactional`을 선언하지 않고 `@DistributedLock`만 유지하여 전체 승인 흐름을 조율합니다.
2. **1단계 — 승인 준비 및 선출금 (`PaymentTransactionService.prepareApproval`)**:
   - 짧은 로컬 `@Transactional` 안에서 결제 상태를 `PENDING` ➔ `APPROVING`으로 변경하고, 구매자 계좌에서 결제 금액을 **선출금(`withdraw`)** 합니다. 트랜잭션이 즉시 커밋되므로 DB 커넥션이 바로 반납됩니다.
3. **외부 PG사 승인 요청 (`pgPort.pay`)**:
   - **DB 트랜잭션 외부**에서 외부 PG API를 호출합니다. 네트워크 지연이 발생해도 DB 커넥션을 전혀 점유하지 않습니다.
4. **2단계 — 최종 확정 또는 보상 트랜잭션**:
   - **성공 시 (`PaymentTransactionService.completeApproval`)**: 짧은 `@Transactional` 안에서 상태를 `APPROVING` ➔ `SUCCESS`로 변경하고 완료 이벤트(Outbox)를 기록합니다.
   - **실패 시 (`PaymentTransactionService.failApproval`)**: 짧은 `@Transactional` 안에서 상태를 `APPROVING` ➔ `FAILED`로 변경하고, 1단계에서 선출금했던 금액을 고객 계좌로 **재입금(`deposit`, 보상 트랜잭션)** 합니다.

### 2.2. 대사 스케줄러 (`ReconciliationScheduler`)
외부 PG 호출 도중 서버가 비정상 종료되어 `APPROVING` 상태로 멈춘(Stuck) 결제 건을 복구합니다.
1. 주기적으로 `APPROVING` 상태이면서 최종 수정 시각(`updatedAt`)이 임계 시간(10초)을 초과한 미결 건을 조회합니다.
2. 외부 PG사의 거래 상태 조회 API(`PgPort.queryStatus`)를 호출하여 실제 결제 성공 여부를 확인합니다.
   - **PG 결제 성공(`SUCCESS`) 확인 시**: `completeApproval`을 호출하여 승인 완료 상태로 복구합니다.
   - **PG 결제 실패(`FAILED` / `NOT_FOUND`) 확인 시**: `failApproval`을 호출하여 실패 전환 및 선출금액 자동 환불을 수행합니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 외부 PG 통신 구간 동안 DB 커넥션을 점유하지 않으므로 처리량(Throughput)과 장애 격리성이 대폭 향상됩니다.
  - 서버 크래시나 네트워크 타임아웃 등 비정상 상황에서도 대사 스케줄러를 통해 계좌 잔액과 결제 상태의 최종 정합성이 보장됩니다.
- **단점 및 트레이드오프**:
  - 로컬 DB 트랜잭션이 1회에서 2회(준비 + 확정/보상)로 나뉘어 DB 커밋 횟수가 증가하지만, 긴 외부 네트워크 I/O 동안 커넥션을 잡고 있는 비용에 비하면 훨씬 안전하고 경제적입니다.
