# ADR-0008: 공통 예외 계층 및 글로벌 예외 처리(`GlobalExceptionHandler`) 설계

- **상태(Status)**: Accepted
- **영향 범위**: `module-common`, `module-domain`, `module-api`

---

## 1. 배경 (Context)
초기 구현에서는 도메인 및 서비스 검증 실패 시 자바 표준 예외(`IllegalArgumentException`, `IllegalStateException`)에 의존하고 있었습니다.
그러나 시스템 규모가 커지고 외부 PG 연동(`module-client`), 분산 락, 멱등성 제어 등 다양한 실패 시나리오가 추가됨에 따라, 예외의 원인과 HTTP 상태 코드를 일관되게 매핑하고 클라이언트에게 표준화된 에러 응답(`ErrorResponse`)을 제공할 공통 예외 체계가 필요해졌습니다.

---

## 2. 결정 사항 (Decision)
모든 모듈이 공유할 수 있는 표준 예외 규격을 `module-common`에 정의하고, 표현 계층(`module-api`)의 `GlobalExceptionHandler`에서 통합 처리합니다.

1. **중앙 집중식 에러 코드 (`ErrorCode` Enum in `module-common`)**:
   - 각 비즈니스/인프라 오류에 대응하는 HTTP 상태 코드(`status`), 고유 식별 코드(`code`), 기본 메시지(`message`)를 `ErrorCode` Enum으로 통합 관리합니다.
2. **공통 비즈니스 예외 (`BusinessException`) 및 도메인 예외 계층**:
   - `RuntimeException`을 상속받고 `ErrorCode`를 보유하는 `BusinessException`을 최상위 커스텀 예외로 정의합니다.
   - `InsufficientBalanceException`, `PaymentNotFoundException`, `InvalidAccountStatusException` 등 도메인 의미를 명확히 드러내는 구체적 예외 클래스를 정의해 가독성과 디버깅 편의성을 높입니다.
3. **전역 예외 핸들러 (`GlobalExceptionHandler` in `module-api`)**:
   - `@RestControllerAdvice`를 통해 `BusinessException`을 일괄 포착하여 `ErrorCode`에 정의된 HTTP 상태 코드와 표준 `ApiResponse` 에러 포맷으로 변환해 응답합니다.
   - Bean Validation(`MethodArgumentNotValidException`), Resilience4j 서킷 오픈(`CallNotPermittedException`), Feign 통신 예외 등 외부 인프라 예외도 내부 구현을 노출하지 않고 일관된 에러 규격으로 감싸 반환합니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 컨트롤러나 퍼사드마다 중복된 `try-catch` 에러 응답 조립 코드가 사라지고, 클라이언트(프론트엔드)가 `code` 필드 기반으로 예측 가능한 에러 분기 처리를 할 수 있습니다.
  - 도메인 모듈(`module-domain`)은 Spring 웹 의존성 없이도 `module-common`의 `BusinessException`을 사용해 명확한 도메인 예외를 던질 수 있습니다.
- **단점 및 고려사항**:
  - 새로운 도메인 기능이 추가될 때마다 `ErrorCode` Enum에 적절한 상태 코드와 에러 코드를 일관성 있게 등록·관리해야 합니다.
