# 0005. 결제 취소 및 환불 (Cancel & Refund) 구현 계획

## 1. 개요 및 목표
현재 은행/결제 시스템에는 결제 생성(`createPayment`)과 승인(`approvePayment`) 기능이 모두 구현되어 있으며, 
성공 시 비동기 이벤트 발행(EDA) 및 서킷 브레이커, 멱등성 처리 등 요구되었던 주요 아키텍처가 반영되어 있습니다.

**다음으로 진행할 비즈니스 기능은 '결제 취소 및 환불(Cancel & Refund)'입니다.**
결제 시스템은 반드시 기 완료된 결제를 취소하고 계좌 잔액을 원상복구(Refund)하는 라이프사이클을 온전히 지원해야 합니다.

---

## 2. 설계 상세

### 2.1 도메인 및 이벤트 정의 (module-domain)
- 기존 `Account`에 있는 `deposit` (입금) 메서드를 활용하여 환불을 처리합니다.
- 기존 `Payment` 모델에 이미 작성된 `cancel()` 메서드를 활용하여 결제 상태를 `CANCELLED`로 변경합니다.
- 환불 처리를 나타내는 도메인 이벤트 `PaymentCanceledEvent`를 새로 정의합니다.
- `PgPort` 인터페이스에 `cancel(orderId: String, amount: Long): Boolean` 메서드를 추가합니다.

### 2.2 외부 PG사 취소 연동 (module-client)
- `PgFeignClient` 인터페이스에 결제 취소 API 엔드포인트(`POST /v1/payments/cancel`)와 DTO를 정의합니다.
- `PgAdapter`에서 `PgPort.cancel`을 구현하고 `@CircuitBreaker` 및 `cancelFallback`을 구성하여 서킷 브레이커를 적용합니다.

### 2.3 애플리케이션 및 API 구현 (module-api)
- `PaymentFacade`에 `cancelPayment` 메서드를 추가하여 취소 흐름을 제어합니다.
  - `@DistributedLock`을 통한 동시성 제어
  - 결제 단건 조회 및 취소 가능 상태 검증
  - `PgPort.cancel` 호출을 통한 외부 PG 취소 진행
  - 취소 성공 시 `Account.deposit`을 이용한 잔액 환불
  - `PaymentHistory` 이력 적재 및 `PaymentCanceledEvent` 발행
- `PaymentController`에 `POST /api/v1/payments/{orderId}/cancel` API 엔드포인트를 추가하고 `@Idempotent`를 적용하여 멱등성을 보장합니다.

---

## 3. 모듈별 수정 및 추가 사항

- **[module-domain]**
  - [MODIFY] `PgPort.kt`: `cancel` 메서드 추가
  - [NEW] `event/PaymentCanceledEvent.kt`: 결제 취소 이벤트 정의

- **[module-client]**
  - [MODIFY] `PgFeignClient.kt`: 취소 API 엔드포인트 및 DTO 추가
  - [MODIFY] `PgAdapter.kt`: `cancel` 메서드 구현 및 서킷 브레이커 적용

- **[module-api]**
  - [MODIFY] `application/PaymentFacade.kt`: `cancelPayment` 비즈니스 로직 작성
  - [MODIFY] `controller/PaymentController.kt`: 취소 API 추가 및 `@Idempotent` 적용
