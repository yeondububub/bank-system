package com.bank.system.api.scheduler

import com.bank.system.api.listener.PaymentNotificationListener
import com.bank.system.domain.OutboxRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxScheduler(
    private val outboxRepository: OutboxRepository,
    private val paymentNotificationListener: PaymentNotificationListener,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_RETRY_COUNT = 5
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun processOutboxMessages() {
        val messages = outboxRepository.findPendingMessages(limit = 100)
        
        if (messages.isEmpty()) {
            return
        }
        
        log.info("미처리된 Outbox 메시지 {}건을 처리합니다.", messages.size)

        for (message in messages) {
            try {
                when (message.eventType) {
                    "PaymentCompletedEvent" -> {
                        val payloadNode: JsonNode = objectMapper.readTree(message.payload)
                        val buyerId = payloadNode.get("buyerId").asLong()
                        val orderId = payloadNode.get("orderId").asText()
                        val amount = payloadNode.get("amount").asLong()
                        
                        paymentNotificationListener.sendPaymentCompletedNotification(buyerId, orderId, amount)
                    }
                    "PaymentCanceledEvent" -> {
                        // 취소 알림이 있다면 여기서 발송
                        log.info("결제 취소 알림 발송 로직 실행 (orderId: {})", message.aggregateId)
                    }
                    else -> {
                        log.warn("알 수 없는 이벤트 타입입니다: {}", message.eventType)
                    }
                }
                message.markAsProcessed()
            } catch (e: Exception) {
                log.error("Outbox 메시지 처리 중 오류 발생 (id: {})", message.id, e)
                message.incrementRetryCount()
                if (message.retryCount >= MAX_RETRY_COUNT) {
                    message.markAsFailed()
                    log.error("🚨 Outbox 메시지 재시도 횟수 초과로 최종 실패 처리합니다. (id: {}, eventType: {}, aggregateId: {})",
                        message.id, message.eventType, message.aggregateId)
                }
            }
        }
        
        outboxRepository.saveAll(messages)
    }
}
