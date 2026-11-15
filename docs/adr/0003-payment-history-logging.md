# ADR-0003: 결제 상태 변경 이력(PaymentHistory) 감사 로깅 설계

- **상태(Status)**: Accepted
- **영향 범위**: `module-domain`, `module-infra`, `module-api`

---

## 1. 배경 (Context)
결제는 `PENDING` ➔ `APPROVING` ➔ `SUCCESS` (또는 `FAILED`, `CANCELLED`)와 같은 상태 머신(State Machine) 흐름을 따릅니다.
만약 `payments` 테이블의 현재 상태(`status`) 컬럼만 덮어쓰기(Update)한다면, 과거 결제가 언제 생성되어 어떤 상태 전이를 거쳐 승인·실패·취소되었는지에 대한 감사(Audit) 추적이 불가능합니다.

금융 및 결제 시스템에서는 정산 대사(Reconciliation), 장애 원인 분석, 고객 분쟁 대응을 위해 모든 상태 전이 내역을 불변(Immutable) 이력으로 보존해야 합니다.

---

## 2. 결정 사항 (Decision)
결제 상태가 변경될 때마다 이를 불변 레코드로 기록하는 **`PaymentHistory` 엔티티**를 도입하고, 결제 상태 변경 트랜잭션과 원자적으로 결합합니다.

1. **이력 도메인 모델 정의 (`PaymentHistory`)**:
   - `paymentId`, `previousStatus`, `currentStatus`, `reason`(변경 사유), `createdAt` 속성을 정의하여 어떤 결제가 왜, 언제, 어떤 상태로 전이되었는지 시계열로 기록합니다.
2. **상태 변경과의 트랜잭션 원자성 보장**:
   - 결제 승인 준비, 승인 확정, 승인 실패 보상, 결제 취소 등 `Payment`의 상태가 전이되는 로컬 DB 트랜잭션 내부에서 `PaymentHistory`를 함께 저장합니다.
   - 이를 통해 결제 상태만 변경되고 이력이 누락되거나, 반대로 이력만 남고 상태 변경이 롤백되는 데이터 불일치를 원천 차단합니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 결제의 전체 라이프사이클을 시계열로 추적할 수 있는 감사 추적성(Auditability)과 데이터 신뢰성이 확보됩니다.
  - 대사(Reconciliation) 및 CS 분쟁 처리 시 명확한 상태 전이 근거를 제공합니다.
- **단점 및 트레이드오프**:
  - 결제 상태가 전이될 때마다 `payment_histories` 테이블에 `INSERT` 쿼리가 함께 발생하므로 쓰기 I/O가 소폭 증가합니다.
