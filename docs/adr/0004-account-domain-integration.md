# ADR-0004: 계좌(Account) 도메인 구축 및 결제·잔액 차감 원자성 보장

- **상태(Status)**: Accepted (Amended by [ADR-0012](0012-two-step-payment-approval.md))
- **영향 범위**: `module-domain`, `module-infra`, `module-api`

---

## 1. 배경 (Context)
결제 승인 과정의 실질적인 재무적 효력은 사용자 계좌(`Account`)의 잔액 차감에서 발생합니다.
만약 결제는 승인되었는데 계좌 잔액이 차감되지 않거나, 반대로 계좌 잔액은 출금되었는데 결제 상태가 반영되지 않는다면 심각한 금전적 정합성 사고로 이어집니다.

따라서 계좌 도메인을 구축하여 입출금 불변식을 도메인 내부에 캡슐화하고, 결제 상태 변경과 계좌 잔액 증감이 원자적(All-or-Nothing)으로 처리되도록 설계해야 합니다.

---

## 2. 결정 사항 (Decision)
1. **계좌 도메인 모델 신설 (`Account`)**:
   - 소유자(`ownerId`), 계좌번호(`accountNumber`), 잔액(`balance`), 계좌 상태(`status`), 대표 계좌 여부(`isPrimary`)를 속성으로 가지며, 출금(`withdraw`)과 입금(`deposit`) 시 잔액 부족 검증(`InsufficientBalanceException`) 및 활성 상태 검증을 도메인 내부에 캡슐화합니다.
2. **계좌 행 단위 비관적 락(`findByIdWithLock`) 지원**:
   - 동일 계좌에 대해 동시에 여러 출금·송금·환불 요청이 경합할 때 갱신 분실(Lost Update)과 이중 출금(Double Spending)을 방지하기 위해 `AccountJpaRepository`에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 기반 조회 메서드를 제공합니다.
3. **결제 상태 전이와 계좌 잔액 변경의 트랜잭션 결합**:
   - 결제 금액 차감(`Account.withdraw`)과 결제 상태 갱신(`Payment`), 그리고 이력 적재(`PaymentHistory`)를 동일한 `@Transactional` 경계 안에서 수행하여 잔액 부족 시 전체 작업이 안전하게 롤백되도록 보장합니다.
   - *(보완 사항)* 초기에는 단일 트랜잭션 내에서 외부 PG 호출까지 함께 수행했으나, 외부 통신 지연으로 인한 DB 커넥션 점유 문제를 해결하기 위해 **[ADR-0012](0012-two-step-payment-approval.md)** 에서 **1단계 선출금 트랜잭션(`prepareApproval`) + 외부 PG 호출 + 2단계 확정/보상 트랜잭션(`completeApproval` / `failApproval`)** 구조로 발전시켰습니다.

---

## 3. 결과 및 영향 (Consequences)
- **장점**:
  - 계좌 입출금 규칙이 `Account` 도메인 엔티티 내부에 응집되어 있어, 결제·송금·환불 등 어디서 호출하더라도 동일한 도메인 검증 규칙이 강제됩니다.
  - 로컬 DB 트랜잭션과 비관적 락을 결합하여 계좌 잔액과 결제 상태 간의 정합성을 보장합니다.
- **단점 및 고려사항**:
  - 복수의 계좌나 엔티티에 동시에 락을 획득할 경우 교착 상태(Deadlock)가 발생할 수 있으므로, 계좌 간 이체 등에서는 항상 **일관된 순서(예: 계좌 ID 오름차순)** 로 락을 획득해야 합니다.
