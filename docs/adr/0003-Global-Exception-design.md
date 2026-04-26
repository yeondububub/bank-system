# Bank-System Backend - Step 3: 글로벌 예외 처리(Global Exception Handler) 고도화 설계

## 1. 개요 및 목표
   현재 시스템은 기본 자바 예외(IllegalArgumentException, IllegalStateException)에 의존하여 에러 메시지를 반환하고 있습니다.<br>
   
시스템 규모가 커지고 외부 모듈(module-client)이 도입됨에 따라, 에러의 출처와 원인을 명확하게 식별하고 클라이언트에게 일관된 ErrorResponse 규격을 제공하기 위한 독자적인 예외(Exception) 계층 구조와 통합 처리기가 필요합니다.

## 2. 설계 및 아키텍처 제안
### 2.1 공통 예외 구조 설계 (module-common)
ErrorCode (Enum): 모든 에러의 메타데이터(HTTP 상태 코드, 커스텀 에러 코드, 기본 메시지)를 중앙 집중적으로 관리합니다.
<br>e.g. INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "E001", "잔액이 부족합니다.")
<br>e.g. EXTERNAL_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E002", "외부 연동 중 오류가 발생했습니다.")

**BusinessException (Base Exception):** RuntimeException을 상속받고 ErrorCode를 필드로 가지는 최상위 커스텀 예외 클래스입니다. 
   
### 2.2 도메인별 구체적 예외 클래스 생성
BusinessException을 상속받는 구체적인 예외들을 생성하여, 코드의 가독성을 높입니다.<br>
(예: PaymentNotFoundException, InsufficientBalanceException 등) 
   
### 2.3 GlobalExceptionHandler 고도화 (module-api)
**BusinessException 통합 핸들러:** 모든 커스텀 예외를 한 곳에서 잡아 ErrorCode에 정의된 HTTP 상태 코드와 메시지로 ErrorResponse를 조립합니다.


**외부 모듈(Feign/Resilience4j) 예외 처리:** module-client에서 발생할 수 있는 서킷 브레이커 차단 예외(CallNotPermittedException)나 외부 API 호출 실패(FeignException)를 잡아 적절한 포맷(예: 503 Service Unavailable)으로 감싸서 반환합니다.

## 3. 모듈별 상세 작업 계획

### module-common
- [NEW] ErrorCode.kt (Enum 클래스) 생성.
- [NEW] BusinessException.kt (최상위 예외 클래스) 생성.
- [NEW] DomainException.kt 등 도메인/비즈니스별 하위 커스텀 예외 생성 (또는 도메인 모듈에 위치).

### module-domain
- [MODIFY] PaymentService.kt, Account.kt 등의 비즈니스 로직 수정. 기존의 IllegalArgumentException 등을 새로 만든 커스텀 예외(PaymentNotFoundException 등)를 던지도록 리팩터링.
   
### module-api
- [MODIFY] GlobalExceptionHandler.kt 리팩터링.
  - BusinessException 핸들링 추가. 
  - CallNotPermittedException (Resilience4j 서킷 오픈 시 예외) 핸들링 추가. 
  - Exception(최상위 500 에러) 로깅 고도화 (에러 트래킹 편의성).