# ADR-0005: Redisson을 활용한 Redis 분산 락(Distributed Lock) 도입

- **상태(Status)**: Accepted
- **영향 범위**: `module-common`, `module-api`

---

## 1. 배경 (Context)
[ADR-0002](0002-pessimistic-lock-concurrency-control.md)에서 도입한 데이터베이스 비관적 락(`SELECT ... FOR UPDATE`)은 DB 트랜잭션이 시작된 후 락을 획득하고 트랜잭션이 끝날 때까지 DB 커넥션을 점유합니다.
그러나 다중 서버(Scale-out) 환경에서 동일 주문(`orderId`)에 대한 중복 승인·취소 요청이나 대사(Reconciliation) 스케줄러 작업이 겹칠 때, 매번 DB 커넥션을 먼저 점유한 채 대기하게 되면 **커넥션 풀 고갈(Connection Starvation)** 위험이 커집니다.

또한 [ADR-0012](0012-two-step-payment-approval.md)와 같이 하나의 비즈니스 흐름이 여러 개의 짧은 DB 트랜잭션과 외부 API 호출로 나뉘는 경우, 단일 DB 트랜잭션에 종속된 비관적 락만으로는 전체 오케스트레이션 구간의 동시성을 보호할 수 없습니다.

---

## 2. 결정 사항 (Decision)
인메모리 데이터 저장소인 Redis와 **Redisson** 클라이언트를 활용하여 선언적 **분산 락(`@DistributedLock`)** AOP를 도입합니다.

1. **Redisson Pub/Sub 기반 락 획득**:
   - Lettuce의 스핀 락(Spin Lock) 방식 대신 Redisson의 Pub/Sub 메커니즘을 사용하여, 락 해제 시 대기 중인 스레드에 이벤트를 통지함으로써 Redis 네트워크 트래픽과 CPU 부하를 최소화합니다.
2. **트랜잭션 바운더리보다 넓은 락 라이프사이클 보장 (`DistributedLockAop`)**:
   - DB 트랜잭션이 커밋되기 전에 분산 락이 먼저 해제되면, 대기하던 다른 스레드가 아직 커밋되지 않은 이전 데이터를 읽는 정합성 문제가 발생합니다.
   - 이를 방지하기 위해 `@DistributedLock` AOP가 `@Transactional`보다 **바깥쪽(먼저 시작하고 나중에 종료)** 에서 동작하도록 구성하여, **락 획득 ➔ 트랜잭션 수행 및 커밋 완료 ➔ 락 해제** 순서를 보장합니다.
3. **SpEL 기반 동적 락 키 바인딩**:
   - `@DistributedLock(key = "#orderId")`와 같이 Spring Expression Language(SpEL)를 통해 메서드 파라미터에서 동적으로 락 키를 추출합니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - DB 진입 이전(애플리케이션 앞단)에서 중복 동시 요청을 제어하므로 DB 커넥션 풀과 로우 락 경합 부하를 크게 낮춥니다.
  - 트랜잭션이 분리된 2단계 결제 승인 프로세스 전체 구간을 주문 단위로 안전하게 보호할 수 있습니다.
- **단점 및 고려사항**:
  - Redis 장애가 단일 장애점(SPOF)이 될 수 있으므로 운영 환경에서는 Redis Sentinel 또는 Cluster 구성이 필요합니다.
  - 외부 API 타임아웃보다 락 점유 시간(`leaseTime`)이 짧으면 작업 도중 락이 자동 해제될 수 있으므로, 외부 통신 타임아웃 합산 시간보다 넉넉하게 `leaseTime`을 설정해야 합니다.
