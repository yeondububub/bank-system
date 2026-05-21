package com.bank.system.domain

interface OutboxRepository {
    fun save(message: OutboxMessage): OutboxMessage
    fun findPendingMessages(limit: Int): List<OutboxMessage>
    fun saveAll(messages: List<OutboxMessage>): List<OutboxMessage>
}
