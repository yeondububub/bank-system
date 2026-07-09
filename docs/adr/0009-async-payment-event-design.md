# Bank-System Backend - Step 9: 이벤트 기반 아키텍처 (EDA) 상세 구현 계획

## 1. 개요 및 목표
현재 문제: 결제가 성공(PaymentService.approvePayment)한 뒤 사용자에게 알림톡(Push/Email)을 보내거나 포인트를 적립해야 한다고 가정해 보겠습니다. 만약 알림 시스템에 장애가 생기거나 느려지면, 결제 트랜잭션 자체가 실패하거나 타임아웃이 발생합니다. 

**해결책:** 결제 로직이 완료되면 DB 커밋 직후에 **"결제 완료 이벤트(PaymentCompletedEvent)"** 만 툭 던지고 응답을 반환합니다. 알림/포인트 로직은 이를 비동기(@Async)로 리스닝하여 처리합니다.

## 2. 설계 상세

### 2.1 이벤트 객체 정의 (module-domain)
PaymentCompletedEvent: orderId, buyerId, amount 등의 정보를 담은 불변 레코드(Record/Data Class).

### 2.2 이벤트 퍼블리셔 적용 (module-api / PaymentFacade)
   ApplicationEventPublisher를 사용하여 결제 승인(approvePayment)이 정상적으로 끝난 직후 이벤트를 발행합니다. 
   
### 2.3 비동기 이벤트 리스너 구현 (module-api 또는 신규 모듈)
- [NEW] PaymentNotificationListener.kt: @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)를 사용하여, DB 커밋이 완벽하게 끝난 이후에만 알림 로직이 동작하게 합니다. 
- @Async 애노테이션을 부착하여 메인 결제 스레드와 전혀 다른 별도의 스레드 풀에서 실행되도록 격리합니다.
   
### 2.4 비동기(Async) 스레드 풀 설정 (module-api)
   @EnableAsync 추가 및 ThreadPoolTaskExecutor (비동기 전용 일꾼) 빈 생성.