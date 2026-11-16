# ADR-0015: JWT 인증, BCrypt 해싱 및 인터셉터 기반 인가 아키텍처

- 상태: Accepted
- 영향 범위: `module-domain`, `module-api`

## 1. 배경

초기 인증 구현은 SHA-256 단순 해시로 비밀번호를 저장하고, 커스텀 파이프 구분자 문자열을 토큰으로 사용했다. 솔트 없는 SHA-256은 레인보우 테이블과 무차별 대입 공격에 취약하며, API 엔드포인트 전반에 일관된 인증 강제 장치가 없어 미인증 요청이나 권한 검증 누락이 발생할 수 있었다.

전역 Spring Security 필터 체인 전체를 도입하는 대신, 현재 프로젝트의 가벼운 MVC 구조와 결합하기 쉬운 명시적 인증 및 인가 파이프라인이 필요했다.

## 2. 결정 사항

### 2.1. BCrypt 비밀번호 해싱과 표준 JSON JWT
- `PasswordEncoder`를 `spring-security-crypto`의 `BCryptPasswordEncoder`로 교체하고, BCrypt의 72바이트 입력 상한을 초과하는 비밀번호는 사전에 거부하도록 방어했다.
- `JwtTokenProvider`를 `header.payload.signature` 3파트 JSON 구조로 개편하고, 서명 비교 시 타이밍 공격을 막기 위해 `MessageDigest.isEqual` 상수 시간 비교를 적용했다.
- 회원가입 요청(`SignUpRequest`)에서 권한과 초기 잔액 입력 필드를 제거해 서버가 `UserRole.USER`와 기본 초기 잔액을 강제하도록 했다.

### 2.2. 인터셉터 기반 전역 인증 및 `@LoginUser` 주입
- `UserAuthenticationInterceptor`를 `WebConfig`의 `/api/v1/**` 경로에 등록하고, 로그인과 회원가입 경로만 예외로 열어 기본 차단(Fail-Closed) 정책을 적용했다.
- `AdminAuthorizationInterceptor`는 `/api/v1/admin/**` 경로에서 `UserRole.ADMIN` 권한을 추가로 검사한다.
- 컨트롤러가 매번 `Authorization` 헤더를 직접 파싱하지 않도록 `@LoginUser` 어노테이션과 `LoginUserArgumentResolver`를 구현해 `AuthenticatedUser` 객체를 선언적으로 주입받게 했다.

## 3. 결과 및 영향

- 인증 책임이 Spring MVC 인터셉터와 리졸버로 분리되어 컨트롤러 코드가 단순해졌고, 신규 API가 추가되어도 기본 인증이 누락되지 않는다.
- Spring Security의 거대한 필터 체인 없이 필요한 보안 요구사항만 가볍게 통제할 수 있지만, 추후 OAuth2 소셜 로그인이나 세션 관리가 필요해지면 Spring Security로의 전환 비용이 발생한다.
- 인터셉터가 확인하는 것은 로그인 여부(인증)와 관리자 경로 여부이므로, 일반 사용자가 타인의 `accountId`나 `orderId`에 접근하지 못하도록 막는 리소스 소유권 검증(BOLA)은 각 컨트롤러와 퍼사드에서 `@LoginUser`의 `userId`를 대조해 처리해야 한다.

