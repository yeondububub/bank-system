package com.bank.system.infra

import com.bank.system.domain.OutboxMessageStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxJpaRepository : JpaRepository<OutboxJpaEntity, Long> {
    fun findByStatusOrderByCreatedAtAsc(status: OutboxMessageStatus, pageable: Pageable): List<OutboxJpaEntity>
}
