# Bank-System Backend - Step 7: 외부 통신 모듈(module-client) 및 PG사 연동 설계

## 1. 개요 및 목표
현재의 bank-system은 내부 데이터베이스 처리와 동시성 제어가 완벽하게 구성되어 있습니다.<br>
이제 외부 결제 대행사(PG사)의 API를 호출하기 위해 시스템을 외부에 연결해야 합니다.<br>

헥사고날 아키텍처의 원칙에 따라, 외부 통신 로직을 철저히 격리하기 위해 새로운 인프라 어댑터인 module-client 모듈을 신설하고, 
외부 장애가 우리 시스템으로 전파되지 않도록 **Circuit Breaker(서킷 브레이커)** 를 도입합니다.

## 2. 설계 및 아키텍처 제안
### 2.1 기술 스택 선택
HTTP Client: `Spring Cloud OpenFeign`<br>
이유: 애노테이션 기반의 선언적 API 호출이 가능하여 코드가 매우 직관적이고 깔끔합니다. (선택지에 따라 WebClient도 가능하지만, 비동기 논블로킹 패러다임 전체 도입이 아니라면 Feign이 유지보수에 좋습니다.)

Circuit Breaker: `Resilience4j`<br>
이유: Spring Cloud CircuitBreaker의 기본 구현체이며, 외부 PG사 API 장애/지연 시 우리 시스템의 스레드가 고갈되는 것을 방지하고 빠른 실패(Fast Fail) 처리를 도와줍니다.

### 2.2 모듈 간 의존성 구조 (Hexagonal)

module-client는 외부 통신을 담당하는 Adapter입니다.<br>
module-domain에서 PgPort(인터페이스)를 정의하고, module-client에서 PgAdapter(구현체) 및 PgFeignClient를 둡니다.<br>
결과적으로 module-domain은 module-client를 전혀 모른 채 인터페이스만 호출하며, module-api 구동 시 컴포넌트 스캔을 통해 어댑터가 주입됩니다.

## 3. 구현 단계 및 모듈별 작업 계획

- 프로젝트 루트
   - [MODIFY] settings.gradle.kts: include("module-client") 추가.
   - [MODIFY] build.gradle.kts (루트 또는 api): Spring Cloud 의존성 관리(BOM) 추가.
   - [module-client] (신설)
   - [NEW] build.gradle.kts: OpenFeign 및 Resilience4j 의존성 추가.
   - [NEW] PgFeignClient: 실제 PG사 API 스펙(가상)에 맞춘 Feign 인터페이스.
   - [NEW] PgAdapter: module-domain의 Port를 구현하며 내부적으로 PgFeignClient를 호출하는 어댑터 클래스.
   - [NEW] Resilience4j 설정 (application.yml 조각 또는 설정 클래스): 실패율 50% 이상 시 서킷 오픈, 타임아웃 3초 등 설정.

- module-domain
   - [NEW] PgPort: 외부 결제 요청에 대한 포트(인터페이스) 정의.
   - [MODIFY] PaymentService: 결제 승인 비즈니스 로직 내에서 PgPort.pay()를 호출하여 실제 PG사 연동 결과를 반영하도록 수정