package com.bank.system.infra

import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.OutboxMessage
import com.bank.system.domain.OutboxMessageStatus
import com.bank.system.domain.OutboxRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class OutboxRepositoryAdapter(
    private val outboxJpaRepository: OutboxJpaRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator
) : OutboxRepository {

    override fun save(message: OutboxMessage): OutboxMessage {
        val targetMessage = if (message.id == null) {
            message.copy(id = snowflakeIdGenerator.nextId())
        } else {
            message
        }
        val entity = OutboxJpaEntity.fromDomain(targetMessage)
        return outboxJpaRepository.save(entity).toDomain()
    }

    override fun findPendingMessages(limit: Int): List<OutboxMessage> {
        val pageable = PageRequest.of(0, limit)
        return outboxJpaRepository.findByStatusOrderByCreatedAtAsc(OutboxMessageStatus.PENDING, pageable)
            .map { it.toDomain() }
    }

    override fun saveAll(messages: List<OutboxMessage>): List<OutboxMessage> {
        val targetMessages = messages.map {
            if (it.id == null) it.copy(id = snowflakeIdGenerator.nextId()) else it
        }
        val entities = targetMessages.map { OutboxJpaEntity.fromDomain(it) }
        return outboxJpaRepository.saveAll(entities).map { it.toDomain() }
    }
}
