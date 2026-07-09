# Bank-System Backend - Step 3: 결제 상태 변경 이력(PaymentHistory) 기록 기능 추가

## 1. 개요 및 배경 (Context)
결제는 `PENDING` ➔ `SUCCESS` ➔ `CANCELED` 등의 복잡한 상태 머신(State Machine) 흐름을 따릅니다.
단순히 `payments` 테이블의 상태 컬럼만 갱신(Update)하게 되면, 과거 결제 요청이 승인되었다가 언제 취소되었는지, 혹은 어떤 상태 전이를 거쳐 실패했는지에 대한 과거 오디팅(Auditing) 기록을 파악할 수 없습니다. 
정산 처리, 결제 장애 분석 및 분쟁 대응 등을 위해 결제 상태 전이의 역사를 불변(Immutable) 상태로 추적할 이력 테이블이 필수적으로 요구됩니다.

## 2. 결정 사항 (Decision)
결제 상태가 변경될 때마다 이를 기록하는 **`PaymentHistory` (상태 변경 이력) 엔티티를 추가**하고, 비즈니스 로직과 강하게 결합시킵니다.

1. **이력 데이터 모델 정의 (`PaymentHistory`)**:
   `paymentId`, `previousStatus`, `currentStatus`, `createdAt`을 속성으로 정의하여, 어떠한 결제가 어떤 상태 변화를 거쳤는지 타임스탬프와 함께 기록할 수 있도록 설계합니다.
2. **트랜잭션 일치와 원자성 보장**:
   `PaymentFacade` 내부에서 결제 승인(`PaymentService.approve`)을 정상 처리하여 결제 상태가 변하는 시점에, `PaymentHistory`를 함께 적재하도록 연동합니다. 결제 상태 갱신과 이력 적재는 하나의 데이터베이스 트랜잭션 내에서 원자적으로 처리되어, 상태만 갱신되거나 이력만 적재되는 불일치 상황을 원천 차단합니다.

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 결제의 라이프사이클을 추적할 수 있는 데이터 신뢰성이 완벽히 확보됩니다.
  - 추후 정산(Reconciliation) 및 CS 처리 시 정확한 시계열 데이터를 제공할 수 있습니다.
- **단점/고려사항**:
  - 결제 승인/취소 요청이 들어올 때마다 `insert` 쿼리가 추가적으로 발생하여 DB 쓰기 부하가 약간 증가할 수 있습니다.
