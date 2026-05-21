package com.bank.system.infra

import com.bank.system.domain.OutboxMessage
import com.bank.system.domain.OutboxMessageStatus
import com.bank.system.domain.OutboxRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class OutboxRepositoryAdapter(
    private val outboxJpaRepository: OutboxJpaRepository
) : OutboxRepository {

    override fun save(message: OutboxMessage): OutboxMessage {
        val entity = OutboxJpaEntity.fromDomain(message)
        return outboxJpaRepository.save(entity).toDomain()
    }

    override fun findPendingMessages(limit: Int): List<OutboxMessage> {
        val pageable = PageRequest.of(0, limit)
        return outboxJpaRepository.findByStatusOrderByCreatedAtAsc(OutboxMessageStatus.PENDING, pageable)
            .map { it.toDomain() }
    }

    override fun saveAll(messages: List<OutboxMessage>): List<OutboxMessage> {
        val entities = messages.map { OutboxJpaEntity.fromDomain(it) }
        return outboxJpaRepository.saveAll(entities).map { it.toDomain() }
    }
}
