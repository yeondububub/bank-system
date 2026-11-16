# ADR-0009: 결제 완료 비동기 이벤트 기반 아키텍처(EDA) 초기 설계

- **상태(Status)**: Superseded by [ADR-0011](0011-transactional-outbox-pattern.md)
- **영향 범위**: `module-domain`, `module-api`

---

## 1. 배경 (Context)
결제 승인이 성공한 뒤 사용자에게 알림(Push/Email)을 발송하거나 부가 처리를 실행할 때, 이를 결제 트랜잭션 내부에서 동기식으로 호출하면 알림 서버의 지연이나 장애가 핵심 결제 트랜잭션의 실패 또는 타임아웃으로 직결되는 강한 결합 문제가 발생합니다.

---

## 2. 결정 사항 (Decision)
초기 설계에서는 Spring의 애플리케이션 이벤트(`ApplicationEventPublisher`)와 비동기 스레드 풀(`@Async`)을 활용하여 결제 핵심 로직과 부가 알림 로직을 분리하기로 결정했습니다.

1. **도메인 이벤트 정의 (`module-domain`)**:
   - `orderId`, `buyerId`, `amount` 등 필요한 최소 정보만 담은 불변 이벤트 객체(`PaymentCompletedEvent`)를 정의합니다.
2. **트랜잭션 커밋 이후 비동기 리스닝 (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`)**:
   - 결제 승인 트랜잭션이 성공적으로 커밋된 직후에만 알림 리스너(`PaymentNotificationListener`)가 실행되도록 `AFTER_COMMIT` 단계를 사용하고, `@Async`와 전용 `ThreadPoolTaskExecutor`를 통해 메인 HTTP 요청 스레드와 실행을 격리합니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 부가 기능(알림 발송 등)의 지연이나 실패가 결제 승인 응답 시간과 트랜잭션 성공 여부에 영향을 주지 않습니다.
- **한계 및 대체 사유**:
  - `AFTER_COMMIT` 이후 메모리 기반 `@Async` 큐로 이벤트를 전달하는 방식은 **DB 커밋 직후 비동기 작업 실행 전 애플리케이션이 비정상 종료(OOM, 배포 재시작 등)될 경우 이벤트가 영구 유실**되는 치명적 약점이 있었습니다.
  - 이에 이벤트 발송의 최소 1회 전송(At-least-once delivery)을 보장하기 위해 **[ADR-0011 (트랜잭셔널 아웃박스 패턴)](0011-transactional-outbox-pattern.md)** 으로 대체되었습니다.
