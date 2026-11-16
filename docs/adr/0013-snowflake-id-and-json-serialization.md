# ADR-0013: Snowflake 64비트 분산 ID 도입 및 JSON 문자열 직렬화

- 상태: Accepted
- 영향 범위: `module-common`, `module-domain`, `module-infra`, `module-api`

## 1. 배경

초기 엔티티 식별자는 MySQL의 `AUTO_INCREMENT`에 의존했다. 이 방식은 DB에 `INSERT`를 실행하기 전까지 ID를 알 수 없고, 샤딩이나 다중 DB 구성으로 확장할 때 식별자가 충돌할 위험이 있다.

ID 생성 책임을 애플리케이션으로 옮기기 위해 64비트 정수 기반 분산 ID를 도입하기로 했다. 이때 JavaScript 기반 프론트엔드(Vue 3)는 `Number.MAX_SAFE_INTEGER`인 `9,007,199,254,740,991`을 넘는 64비트 정수를 JSON으로 파싱할 때 하위 자릿수가 손실되는 문제가 함께 드러났다.

## 2. 결정 사항

### 2.1. Snowflake 64비트 ID 생성기 도입
- `module-common`에 `SnowflakeIdGenerator`를 구현해 타임스탬프, 노드 ID, 시퀀스 번호를 조합한 64비트 `Long` ID를 발급한다.
- 계좌, 사용자, 결제, 아웃박스 메시지 등 주요 엔티티는 영속화 전에 애플리케이션 계층에서 ID를 먼저 할당받는다.
- DB 인덱스 단편화를 줄이기 위해 UUID v4 같은 무작위 문자열 대신 시간순 정렬이 가능한 64비트 정수를 채택했다.

### 2.2. DTO 식별자 필드의 JSON 문자열 직렬화
- 백엔드 DB와 도메인 모델은 저장 효율과 인덱스 성능을 위해 `Long` 타입을 유지한다.
- API 응답 DTO(`AccountResponse`, `PaymentResponse`, `UserResponse`, `AccountTransactionResponse` 등)의 Snowflake ID 필드에는 `@JsonSerialize(using = ToStringSerializer::class)`를 적용해 JSON 응답에서 문자열로 직렬화한다.

## 3. 결과 및 영향

- 애플리케이션이 DB 왕복 없이 고유 ID를 즉시 생성하므로, 헥사고날 도메인 객체가 영속성 컨텍스트에 들어가기 전에도 완전한 식별자를 가질 수 있다.
- 프론트엔드에서 계좌 ID나 거래 ID의 끝자리가 `0`으로 뭉개지던 정밀도 손실 버그가 사라졌다.
- 외부 클라이언트는 ID 필드를 숫자가 아닌 문자열로 다루어야 하며, 신규 응답 DTO를 추가할 때마다 `ToStringSerializer` 누락 여부를 확인해야 한다.

