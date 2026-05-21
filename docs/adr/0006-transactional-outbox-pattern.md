# 트랜잭셔널 아웃박스 패턴(Transactional Outbox Pattern) 도입

## 1. 배경 (Context)
이전 문서([0004-async-payment-event-design.md](0004-async-payment-event-design.md))에서 결제 성공 후 알림 발송 로직을 비동기 이벤트(`@TransactionalEventListener(AFTER_COMMIT)`)로 분리하여 결제 트랜잭션의 응답 속도와 격리성을 확보했습니다.
하지만 기존 방식에는 **데이터 유실(Data Loss)**에 대한 취약점이 존재합니다. DB 커밋은 정상적으로 완료되었지만, 비동기로 알림 API를 호출(또는 Kafka로 전송)하기 직전에 애플리케이션이 크래시(OOM, 배포로 인한 셧다운 등)되면 이벤트가 영구적으로 유실되어 사용자는 결제 알림을 받지 못하게 됩니다.

## 2. 결정 (Decision)
이벤트의 **최소 1회 전송(At-least-once delivery)**을 보장하기 위해 **트랜잭셔널 아웃박스 패턴(Transactional Outbox Pattern)**을 도입하기로 결정했습니다.

1. **Outbox 테이블 추가**: 결제 비즈니스 로직(예: 결제 정보 저장)이 실행되는 동일한 데이터베이스 내에 `outbox_messages` 테이블을 추가합니다.
2. **트랜잭션 결합**: 결제 완료 및 취소 이벤트 발생 시, 외부 API를 바로 호출하지 않고 `@TransactionalEventListener(BEFORE_COMMIT)`을 사용하여 이벤트 데이터를 `outbox_messages` 테이블에 저장합니다. 이를 통해 비즈니스 로직 성공과 이벤트 기록이 하나의 트랜잭션으로 원자적(Atomic)으로 보장됩니다.
3. **폴링 스케줄러**: 백그라운드 스케줄러(`OutboxScheduler`)가 `PENDING` 상태의 아웃박스 메시지를 정기적으로 폴링하여 알림을 발송하고, 성공 시 `PROCESSED`로 상태를 갱신합니다.

## 3. 결과 (Consequences)
- **장점**: 
  - 애플리케이션 장애 시에도 데이터베이스에 이벤트가 안전하게 기록되어 있어, 재시작 시점에 스케줄러를 통해 이벤트를 재처리할 수 있습니다. (안정성 극대화)
  - 결제 시스템은 이벤트 발송의 성공/실패 여부에 전혀 영향을 받지 않으며 완벽하게 격리됩니다.
- **단점/고려사항**:
  - `outbox_messages` 테이블의 지속적인 폴링으로 인한 DB 부하가 발생할 수 있습니다. (추후 데이터량이 많아질 경우 Debezium 등 CDC 툴 도입 고려)
  - 다중 서버 환경(Scale-out)에서는 스케줄러가 동시에 실행되어 알림이 중복 발송될 수 있으므로, DB 비관적 락(`FOR UPDATE SKIP LOCKED`)이나 스케줄러 락(ShedLock) 도입이 필요합니다.
  - 처리 완료된(PROCESSED) 데이터가 테이블에 누적되므로, 별도의 배치 작업을 통한 데이터 주기적 삭제 로직이 필요합니다.
