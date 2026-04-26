package com.bank.system.api.listener

import com.bank.system.domain.event.PaymentCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentNotificationListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompletedEvent(event: PaymentCompletedEvent) {
        log.info("🔔 [비동기 알림] 결제가 성공적으로 완료되었습니다. (orderId: {}, buyerId: {}, amount: {})",
            event.orderId, event.buyerId, event.amount)
        
        try {
            // 알림톡 발송 API 호출 시뮬레이션 (1초 소요)
            Thread.sleep(1000)
            log.info("✅ [비동기 알림] 사용자 {}에게 결제 완료 카카오톡 알림 발송 완료!", event.buyerId)
        } catch (e: InterruptedException) {
            log.error("알림 발송 중 인터럽트 발생", e)
        }
    }
}
