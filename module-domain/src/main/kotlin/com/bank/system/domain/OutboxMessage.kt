package com.bank.system.domain

import java.time.LocalDateTime

class OutboxMessage(
    val id: Long? = null,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    var status: OutboxMessageStatus = OutboxMessageStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var processedAt: LocalDateTime? = null,
    var retryCount: Int = 0
) {
    fun markAsProcessed() {
        this.status = OutboxMessageStatus.PROCESSED
        this.processedAt = LocalDateTime.now()
    }
    
    fun markAsFailed() {
        this.status = OutboxMessageStatus.FAILED
    }

    fun incrementRetryCount() {
        this.retryCount++
    }
}
