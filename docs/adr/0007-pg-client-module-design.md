# ADR-0007: 외부 PG 연동 모듈(`module-client`) 및 서킷 브레이커 설계

- **상태(Status)**: Accepted
- **영향 범위**: `module-domain`, `module-client`, `module-api`, `module-pg`

---

## 1. 배경 (Context)
결제 시스템은 실제 카드사/결제 대행사(PG)와의 외부 HTTP 통신이 필수적입니다.
외부 PG 연동 코드가 `module-domain`이나 `module-api`에 직접 혼재되면 도메인 순수성이 훼손될 뿐 아니라, 외부 PG 서버의 응답 지연이나 장애가 발생했을 때 애플리케이션 스레드 풀이 고갈되어 전체 서비스 장애로 전파(Cascading Failure)될 위험이 큽니다.

---

## 2. 결정 사항 (Decision)
헥사고날 아키텍처의 **포트와 어댑터(Ports & Adapters)** 원칙에 따라 외부 통신 전용 모듈 `module-client`를 신설하고, 장애 격리를 위해 **Resilience4j 서킷 브레이커**를 적용합니다.

### 2.1. 헥사고날 포트-어댑터 격리
1. **Outbound Port (`PgPort` in `module-domain`)**:
   - 도메인 계층은 외부 HTTP 라이브러리(Feign 등)를 전혀 알지 못한 채 `PgPort` 인터페이스(`pay`, `cancel`, `queryStatus`)에만 의존합니다.
2. **Outbound Adapter (`PgAdapter` & `PgFeignClient` in `module-client`)**:
   - 선언적 HTTP 클라이언트인 `Spring Cloud OpenFeign`(`PgFeignClient`)을 사용해 실제 PG API와 통신하고, `PgAdapter`가 `PgPort`를 구현하여 외부 응답을 도메인 DTO로 변환합니다.
   - 애플리케이션 계층의 `PaymentFacade`가 `PgPort`를 주입받아 외부 결제 승인·취소·조회를 오케스트레이션합니다.

### 2.2. 서킷 브레이커(Circuit Breaker) 및 타임아웃 방어
- `PgAdapter`의 외부 호출 메서드에 Resilience4j `@CircuitBreaker`와 `Fallback` 메서드를 적용합니다.
- 외부 PG 호출에 명시적 Connect/Read 타임아웃을 설정하고, 실패율이 임계치를 초과하면 서킷을 오픈(Open)하여 즉시 Fallback으로 빠른 실패(Fast Fail)를 유도함으로써 스레드 풀 고갈을 방지합니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 향후 PG사가 변경되거나 복수 PG사 라우팅이 필요해져도 `module-domain`이나 `module-api` 수정 없이 `module-client`의 어댑터 구현체만 교체·확장하면 됩니다.
  - 외부 PG 장애 시에도 서킷 브레이커가 장애 전파를 차단해 계좌 조회 등 타 핵심 기능의 가용성을 보호합니다.
- **단점 및 고려사항**:
  - 네트워크 타임아웃이나 서킷 오픈으로 인한 실패가 발생했을 때, 실제 PG사에서는 결제가 승인되었을 수 있으므로 [ADR-0012](0012-two-step-payment-approval.md)의 **대사(Reconciliation) 스케줄러**와 함께 동작하여 최종 정합성을 보장해야 합니다.
