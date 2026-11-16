# ADR-0010: 결제 취소 및 계좌 환불(Cancel & Refund) 아키텍처 설계

- **상태(Status)**: Accepted
- **영향 범위**: `module-domain`, `module-client`, `module-pg`, `module-api`

---

## 1. 배경 (Context)
결제 생성(`PENDING`)과 승인(`SUCCESS`)만으로는 완전한 상거래 라이프사이클을 지원할 수 없습니다.
이미 승인이 완료된 결제 건에 대해 사용자가 취소를 요청할 경우, 외부 PG사 결제 취소와 내부 계좌 잔액 환불(원복), 그리고 취소 이력 및 아웃박스 이벤트 발행까지 일관되게 처리하는 취소 아키텍처가 필요했습니다.

---

## 2. 결정 사항 (Decision)
기존 결제 승인 파이프라인과 동일한 동시성·멱등성·장애 격리 원칙을 적용하여 `PaymentFacade.cancelPayment` 및 `POST /api/v1/payments/{orderId}/cancel` API를 구축합니다.

1. **도메인 상태 전이 및 환불 규칙 캡슐화 (`module-domain`)**:
   - `Payment.cancel()` 메서드를 통해 `SUCCESS` 상태인 결제만 `CANCELLED`로 전이할 수 있도록 도메인 불변식을 강제합니다.
   - 환불은 `Account.deposit(amount)` 도메인 메서드를 재사용해 잔액을 원복하고, `PaymentCanceledEvent` 도메인 이벤트를 정의합니다.
2. **외부 PG 취소 연동 (`module-client` & `module-pg`)**:
   - `PgPort.cancel(orderId, amount)` 포트와 `PgFeignClient` 취소 엔드포인트(`POST /v1/payments/cancel`)를 추가하고, `PgAdapter`에 `@CircuitBreaker` 및 Fallback을 적용해 외부 취소 장애가 시스템 전체로 전파되지 않도록 격리합니다.
3. **동시성 제어 및 멱등성 보장 (`module-api`)**:
   - `PaymentController`의 취소 엔드포인트에 `@Idempotent`를 적용해 중복 취소 요청을 차단합니다.
   - `PaymentFacade.cancelPayment`에 `@DistributedLock`을 적용하여 동일 주문에 대한 중복 취소나 승인·취소 간 경합을 방지하고, 취소 성공 시 `PaymentHistory` 적재와 `PaymentCanceledEvent` 발행을 함께 처리합니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 결제 생성 ➔ 승인 ➔ 취소/환불로 이어지는 전체 결제 생명주기를 헥사고날 포트-어댑터 구조 안에서 일관성 있게 완성했습니다.
  - 외부 PG 취소 실패 시 내부 계좌 환불 및 상태 변경이 실행되지 않으므로 불필요한 오환불을 방지합니다.
- **단점 및 고려사항**:
  - 외부 PG 취소 호출 이후 로컬 DB 환불 커밋 직전에 장애가 발생할 경우에도 데이터 정합성을 유지하려면, 취소 진행 중 상태(`CANCELLING`) 도입이나 대사 스케줄러 확장, 그리고 결제 시 출금된 정확한 `accountId` 영속화(추후 개선 예정)가 필요합니다.
