# ADR-0011: 트랜잭셔널 아웃박스 패턴(Transactional Outbox Pattern) 및 DLQ 도입

- **상태(Status)**: Accepted (Supersedes [ADR-0009](0009-async-payment-event-design.md))
- **영향 범위**: `module-domain`, `module-infra`, `module-api`

---

## 1. 배경 (Context)
[ADR-0009](0009-async-payment-event-design.md)에서는 결제 완료 후 알림 발송 로직을 `@TransactionalEventListener(phase = AFTER_COMMIT)`와 `@Async`로 분리하여 결제 트랜잭션의 응답 속도와 결합도를 낮추었습니다.

하지만 `AFTER_COMMIT` 메모리 이벤트 방식에는 **메시지 유실(Data Loss)** 취약점이 존재합니다. 로컬 DB 커밋이 완료된 직후 비동기 스레드가 외부 알림 API나 메시지 브로커(Kafka 등)를 호출하기 전에 애플리케이션이 비정상 종료(OOM, 배포 중 강제 종료 등)되면 해당 이벤트는 영구적으로 유실됩니다.

---

## 2. 결정 사항 (Decision)
도메인 이벤트의 **최소 1회 전송(At-least-once delivery)** 을 보장하기 위해 **트랜잭셔널 아웃박스 패턴(Transactional Outbox Pattern)** 과 **최대 재시도 제한(DLQ 성격의 격리)** 을 도입합니다.

### 2.1. 동일 트랜잭션 내 아웃박스 기록 (`OutboxEventListener`)
1. **Outbox 테이블(`outbox_messages`) 신설**:
   - `id`, `aggregateType`, `aggregateId`, `eventType`, `payload`(JSON), `status`(`PENDING`, `PROCESSED`, `FAILED`), `retryCount` 컬럼을 가진 영속 테이블을 구성합니다.
2. **`BEFORE_COMMIT` 트랜잭션 결합**:
   - 결제 완료(`PaymentCompletedEvent`) 및 취소(`PaymentCanceledEvent`) 이벤트 발행 시, `OutboxEventListener`가 `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` 시점에 동작하여 결제 상태 변경과 동일한 로컬 DB 트랜잭션 안에서 `outbox_messages` 레코드를 `PENDING` 상태로 저장합니다.
   - 이로써 **"결제 상태 변경 커밋"과 "아웃박스 이벤트 저장"의 원자성(Atomicity)** 이 100% 보장됩니다.

### 2.2. 폴링 발행기 및 실패 격리 (`OutboxScheduler`)
1. **주기적 폴링 및 발송**:
   - 백그라운드 스케줄러(`OutboxScheduler`)가 `PENDING` 상태의 아웃박스 메시지를 조회하여 외부 발송을 수행하고, 성공 시 `PROCESSED` 상태로 전환합니다.
2. **재시도 임계값 및 실패 격리 (`MAX_RETRY_COUNT = 5`)**:
   - 일시적 장애 시 `retryCount`를 1씩 증가시키며 재시도하되, 포맷 오류나 장기 장애로 인해 무한 재시도 루프에 빠지는 것(Poison Pill)을 막기 위해 5회 이상 실패 시 상태를 `FAILED`로 전환하고 경고 로그를 남깁니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 서버 크래시나 외부 알림 서버 장애가 발생하더라도 이벤트가 DB에 안전하게 영속화되어 있으므로 유실 없이 재처리됩니다.
  - `FAILED` 상태 격리를 통해 특정 불량 메시지가 전체 아웃박스 큐 처리를 지연시키는 현상을 방지합니다.
- **단점 및 고려사항**:
  - 주기적인 DB 폴링 비용이 발생하며, 다중 인스턴스 환경에서는 중복 발송 방지를 위해 `FOR UPDATE SKIP LOCKED` 또는 ShedLock 적용이 필요합니다.
  - 수신 측(Consumer)에서도 최소 1회 전송 특성상 발생할 수 있는 중복 메시지를 멱등하게 처리해야 합니다.
