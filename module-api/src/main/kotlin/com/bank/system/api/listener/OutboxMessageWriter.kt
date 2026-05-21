package com.bank.system.api.listener

import com.bank.system.domain.OutboxMessage
import com.bank.system.domain.OutboxRepository
import com.bank.system.domain.event.PaymentCanceledEvent
import com.bank.system.domain.event.PaymentCompletedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OutboxMessageWriter(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun onPaymentCompleted(event: PaymentCompletedEvent) {
        log.info("결제 완료 이벤트 발생. Outbox에 저장합니다. (orderId: {})", event.orderId)
        
        val message = OutboxMessage(
            aggregateType = "PAYMENT",
            aggregateId = event.paymentId.toString(),
            eventType = "PaymentCompletedEvent",
            payload = objectMapper.writeValueAsString(event)
        )
        
        outboxRepository.save(message)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun onPaymentCanceled(event: PaymentCanceledEvent) {
        log.info("결제 취소 이벤트 발생. Outbox에 저장합니다. (orderId: {})", event.orderId)
        
        val message = OutboxMessage(
            aggregateType = "PAYMENT",
            aggregateId = event.paymentId.toString(),
            eventType = "PaymentCanceledEvent",
            payload = objectMapper.writeValueAsString(event)
        )
        
        outboxRepository.save(message)
    }
}
