# 1. Step 1: API 멱등성(Idempotency) 보장 로직 구조 설계 및 아이디어
   네트워크 지연이나 클라이언트의 중복 클릭 등으로 인한 중복 결제 요청을 방어하기 위해 Redis 기반의 멱등성 처리 로직을 추가합니다.

## 1.1. 설계 방향성 (AOP 기반)
클라이언트가 HTTP 헤더에 Idempotency-Key (예: UUID)를 포함하여 요청하도록 강제합니다. Spring Interceptor나 Filter를 사용하는 방법도 있지만, 기존 @DistributedLock처럼 AOP 방식의 커스텀 애노테이션을 활용하면 특정 컨트롤러 메서드에만 선언적으로 멱등성을 적용할 수 있어 응집도가 높아지고 우아한 코드가 됩니다.

## 1.2. 제안하는 처리 흐름
클라이언트 요청: POST /api/v1/payments (Header: Idempotency-Key: <UUID>)
AOP 인터셉트 (@Idempotent):
요청 헤더에서 Idempotency-Key를 추출합니다.
Redis에 해당 키로 값을 조회합니다. (SETNX 명령어 또는 Redisson Bucket 활용)


상태 검증:
키가 없는 경우 (최초 요청): Redis에 상태를 PROCESSING으로 저장하고 만료 시간(TTL, 예: 24시간)을 설정한 뒤 비즈니스 로직을 수행합니다.
키가 존재하고 상태가 PROCESSING인 경우 (동시 중복 요청): 즉시 예외 발생 (예: 409 Conflict, "요청이 처리 중입니다").
키가 존재하고 상태가 DONE인 경우 (완료된 중복 요청): 기존에 성공했던 응답을 반환하거나, 중복 요청임을 알리는 예외/응답을 처리합니다.
결과 반환: 비즈니스 로직(Facade) 처리가 끝나면 Redis의 상태를 DONE으로 업데이트합니다. (선택적으로 처리 결과 Response 본문을 Redis에 캐싱하여 재응답할 수도 있습니다.)


## 1.3. 모듈별 수정 및 추가 예정 사항

- [module-common]
  - [NEW] @Idempotent 커스텀 애노테이션 추가: 컨트롤러 메서드에 적용할 수 있도록 만듭니다.
  - [MODIFY] 공통 에러 코드: 멱등성 충돌 시 사용할 예외 규격을 추가합니다.

- [module-api]
  - [NEW] IdempotencyAop (Aspect): @Idempotent가 붙은 메서드를 가로채어 HttpServletRequest에서 헤더를 읽고, Redis를 통해 멱등성을 검사하는 핵심 로직 구현.
  - [MODIFY] PaymentController: 결제 승인 API에 @Idempotent 애노테이션 적용.