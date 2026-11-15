# ADR-0002: 비관적 락(Pessimistic Lock)을 이용한 초기 결제 승인 동시성 제어

- **상태(Status)**: Superseded by [ADR-0005](0005-redis-distributed-lock.md) & [ADR-0012](0012-two-step-payment-approval.md)
- **영향 범위**: `module-infra`, `module-api`

---

## 1. 배경 (Context)
동일한 주문번호(`orderId`)에 대해 네트워크 지연이나 클라이언트의 중복 클릭 등으로 인해 여러 건의 결제 승인 요청이 거의 동시에 유입될 수 있습니다.
만약 동시성 제어를 하지 않는다면, 여러 스레드가 동시에 결제 상태 검증(`status == PENDING`)을 통과하여 동일 결제 건에 대해 이중 승인이 발생하거나 데이터 정합성이 깨질 수 있습니다.

---

## 2. 결정 사항 (Decision)
초기 아키텍처에서는 가장 보수적이고 확실하게 데이터 정합성을 보장하기 위해 데이터베이스 수준의 **비관적 쓰기 잠금(Pessimistic Write Lock, `SELECT ... FOR UPDATE`)** 을 도입했습니다.

1. **DB 행 잠금(Row Lock) 적용**:
   - `PaymentJpaRepository` 인터페이스에 `@Lock(LockModeType.PESSIMISTIC_WRITE)`가 선언된 `findByOrderIdWithLock` 메서드를 추가하여, 결제 승인 로직 진입 시 해당 결제 행(Row)의 배타적 락을 획득합니다.
2. **트랜잭션 및 락 라이프사이클 분리 (`PaymentFacade` 도입)**:
   - 도메인 서비스(`PaymentService`)는 순수한 도메인 상태 변경만 담당하게 하고, 기술적 트랜잭션 바운더리와 락의 수명을 조율하기 위해 `module-api`에 `PaymentFacade` 계층을 도입했습니다.
   - **락 획득 ➔ 도메인 비즈니스 로직(승인 상태 변경) ➔ 데이터베이스 저장 및 커밋**이 단일 트랜잭션 안에서 원자적으로 수행되도록 구성했습니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 별도의 외부 인프라(Redis 등) 없이도 RDBMS의 배타적 행 잠금을 통해 다중 서버 환경에서 동시 수정 유실(Lost Update)을 확실하게 차단합니다.
- **단점 및 이후 아키텍처 진화**:
  - DB 트랜잭션 안에서 락을 대기하고 유지하므로, 트래픽이 몰리거나 외부 PG API 호출처럼 지연이 긴 작업이 결합되면 **DB 커넥션 풀 고갈(Connection Starvation)** 위험이 발생합니다.
  - 이를 해결하기 위해 이후 주문 단위 동시성 제어는 **[ADR-0005 (Redisson 기반 Redis 분산 락)](0005-redis-distributed-lock.md)** 으로 이관하고, 외부 API 호출 구간은 **[ADR-0012 (2단계 결제 승인 프로세스)](0012-two-step-payment-approval.md)** 로 분리하여 고도화했습니다. (단, DB 비관적 락은 계좌 잔액 차감/이체 등 짧은 로컬 트랜잭션의 행 정합성 보호 용도로 계속 활용됩니다.)
