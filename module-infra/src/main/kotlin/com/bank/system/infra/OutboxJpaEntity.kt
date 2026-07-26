package com.bank.system.infra

import com.bank.system.domain.OutboxMessage
import com.bank.system.domain.OutboxMessageStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_messages")
class OutboxJpaEntity(
    @Id
    val id: Long,

    @Column(nullable = false)
    val aggregateType: String,

    @Column(nullable = false)
    val aggregateId: String,

    @Column(nullable = false)
    val eventType: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxMessageStatus = OutboxMessageStatus.PENDING,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var processedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var retryCount: Int = 0
) {
    fun toDomain(): OutboxMessage {
        return OutboxMessage(
            id = id,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            payload = payload,
            status = status,
            createdAt = createdAt,
            processedAt = processedAt,
            retryCount = retryCount
        )
    }

    companion object {
        fun fromDomain(domain: OutboxMessage): OutboxJpaEntity {
            return OutboxJpaEntity(
                id = requireNotNull(domain.id) { "Outbox ID는 필수입니다." },
                aggregateType = domain.aggregateType,
                aggregateId = domain.aggregateId,
                eventType = domain.eventType,
                payload = domain.payload,
                status = domain.status,
                createdAt = domain.createdAt,
                processedAt = domain.processedAt,
                retryCount = domain.retryCount
            )
        }
    }
}
