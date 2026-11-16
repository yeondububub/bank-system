# Bank System (은행 및 결제 시스템)

이 프로젝트는 Kotlin과 Spring Boot를 기반으로 구현된 **고가용성 및 회복 탄력성(Resiliency)을 고려한 은행 및 결제 백엔드 시스템**입니다. 

중복 결제를 방지하는 멱등성 보장, 분산 환경에서의 동시성 제어, 외부 장애의 전파를 막는 서킷 브레이커, 그리고 이벤트 유실을 방지하는 트랜잭셔널 아웃박스 패턴 등이 반영된 견고한 백엔드 아키텍처를 지향합니다.

---

## Tech Stack

- **Language:** Kotlin
- **Framework:** Spring Boot 3.x
- **Build Tool:** Gradle (Kotlin DSL)
- **Database:** MySQL 8.0
- **Cache & Locking:** Redis (alpine) / Redisson
- **HTTP Client:** Spring Cloud OpenFeign
- **Resiliency:** Resilience4j (Circuit Breaker)
- **Architecture:** 멀티 모듈 아키텍처 (Hexagonal Architecture 지향)

---

## 모듈 구조 (Multi-Module Architecture)

헥사고날 아키텍처(Hexagonal Architecture / Ports & Adapters) 디자인에 따라 핵심 비즈니스 도메인 계층이 인프라스트럭처 계층에 의존하지 않도록 의존성을 격리하였습니다.

```
bank-system/
├── module-api/       # 표현 계층 (Controller, Scheduler, Interceptors)
├── module-client/    # 외부 시스템 연동 Adapter (FeignClient, Circuit Breaker)
├── module-domain/    # 핵심 비즈니스 도메인 및 Port 정의 (Payment, Account, PgPort 등)
├── module-infra/     # 데이터베이스 영속성 영역 (JPA Entity, Repository Adapter)
└── module-common/    # 공통 유틸리티 (Idempotency AOP, 예외 규격 등)
```

---

## 핵심 아키텍처 및 구현 기능

### 1. 멱등성 보장 (API Idempotency)
- 클라이언트의 중복 클릭이나 네트워크 재시도로 인한 중복 결제 승인/취소를 방지합니다.
- HTTP 헤더의 `Idempotency-Key`를 가로채는 커스텀 AOP 애노테이션(`@Idempotent`)을 개발하여 적용하였습니다.
- Redis를 통해 최초 요청 시 `PROCESSING` 상태로 락을 획득하고, 완료 시 `DONE`으로 업데이트하여 동시 중복 요청을 원천 차단합니다.

### 2. 동시성 제어 (Concurrency Control)
- 하나의 계좌에 대해 동시에 여러 결제 요청이나 환불 요청이 발생할 경우 발생할 수 있는 데이터 정합성 문제를 방지합니다.
- Redisson 기반의 분산 락 애노테이션(`@DistributedLock`)을 구현하여 주문ID(orderId) 및 사용자 계좌 단위로 임계 영역(Critical Section)을 안전하게 격리합니다.

### 3. 외부 장애 차단 (Circuit Breaker)
- 외부 PG사(결제 대행사) API 호출 지연 또는 장애 발생 시 우리 시스템의 스레드가 고갈되어 전체 시스템이 마비되는 현상을 방지합니다.
- `Spring Cloud OpenFeign`과 `Resilience4j`를 활용하여 외부 PG 모듈(`module-client`)의 실패율이 일정 기준을 초과하면 즉시 Fallback을 실행(서킷 오픈)하도록 구성하여 빠른 실패(Fast Fail)를 유도합니다.

### 4. 트랜잭셔널 아웃박스 패턴 (Transactional Outbox Pattern)
- 결제 완료 및 취소 발생 시 외부 시스템으로의 이벤트 전송(알림 서비스, Kafka 등)에 대한 **최소 1회 전송(At-least-once delivery)**을 보장합니다.
- `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)`를 사용해 결제 정보 저장과 동시에 `outbox_messages` 테이블에 페이로드를 동일한 트랜잭션으로 안전하게 저장합니다.
- 백그라운드 스케줄러(`OutboxScheduler`)가 `PENDING` 상태의 아웃박스 메시지를 지속적으로 폴링 및 발송하고, 성공 시 `PROCESSED`로 업데이트하여 서버 다운 등의 상황에서도 이벤트 유실을 방지합니다.

---

## 설계 문서 (ADR)
시스템의 주요 아키텍처 결정과 진화 과정은 `docs/adr` 경로에 기록되어 있습니다.

| 번호 | 제목 | 상태 |
| :--- | :--- | :--- |
| ADR-0001 | [헥사고날 아키텍처 기반 멀티 모듈 구조 및 결제 도메인 설계](docs/adr/0001-multi-module-architecture.md) | Accepted (Amended by 0007) |
| ADR-0002 | [비관적 락을 이용한 초기 결제 승인 동시성 제어](docs/adr/0002-pessimistic-lock-concurrency-control.md) | Superseded by 0005, 0012 |
| ADR-0003 | [결제 상태 변경 이력(PaymentHistory) 감사 로깅 설계](docs/adr/0003-payment-history-logging.md) | Accepted |
| ADR-0004 | [계좌(Account) 도메인 구축 및 결제·잔액 차감 원자성 보장](docs/adr/0004-account-domain-integration.md) | Accepted (Amended by 0012) |
| ADR-0005 | [Redisson을 활용한 Redis 분산 락 도입](docs/adr/0005-redis-distributed-lock.md) | Accepted |
| ADR-0006 | [Redis 기반 API 멱등성(Idempotency) 보장 설계](docs/adr/0006-api-idempotency-design.md) | Accepted |
| ADR-0007 | [외부 PG 연동 모듈(module-client) 및 서킷 브레이커 설계](docs/adr/0007-pg-client-module-design.md) | Accepted |
| ADR-0008 | [공통 예외 계층 및 글로벌 예외 처리 설계](docs/adr/0008-global-exception-design.md) | Accepted |
| ADR-0009 | [결제 완료 비동기 이벤트 기반 아키텍처 초기 설계](docs/adr/0009-async-payment-event-design.md) | Superseded by 0011 |
| ADR-0010 | [결제 취소 및 계좌 환불 아키텍처 설계](docs/adr/0010-payment-cancellation-design.md) | Accepted |
| ADR-0011 | [트랜잭셔널 아웃박스 패턴 및 DLQ 도입](docs/adr/0011-transactional-outbox-pattern.md) | Accepted |
| ADR-0012 | [2단계 결제 승인 프로세스 및 대사(Reconciliation) 시스템 도입](docs/adr/0012-two-step-payment-approval.md) | Accepted |
| ADR-0013 | [Snowflake 64비트 분산 ID 도입 및 JSON 문자열 직렬화](docs/adr/0013-snowflake-id-and-json-serialization.md) | Accepted |
| ADR-0014 | [계좌 승인 상태 머신, 1인 다계좌 및 송금 거래 원장 설계](docs/adr/0014-multi-account-and-transfer-ledger.md) | Accepted |
| ADR-0015 | [JWT 인증, BCrypt 해싱 및 인터셉터 기반 인가 아키텍처](docs/adr/0015-jwt-authentication-and-authorization.md) | Accepted |


---

## 시작 가이드 (How to Run)

### 1. 인프라 서비스 실행 (Docker Compose)
프로젝트 루트 디렉토리에서 DB(MySQL) 및 캐시/락(Redis) 컨테이너를 구동합니다.
```bash
docker-compose up -d
```

### 2. 프로젝트 빌드
```bash
./gradlew clean build
```

### 3. 애플리케이션 실행
`module-api` 모듈의 메인 클래스를 실행합니다.
```bash
./gradlew :module-api:bootRun
```
