package com.bank.system.infra

import com.bank.system.domain.PaymentHistory
import com.bank.system.domain.PaymentHistoryRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentHistoryRepositoryAdapter(
    private val jpaRepository: PaymentHistoryJpaRepository
) : PaymentHistoryRepository {

    override fun save(history: PaymentHistory): PaymentHistory {
        val entity = PaymentHistoryJpaEntity(
            paymentId = history.paymentId,
            fromStatus = history.fromStatus,
            toStatus = history.toStatus,
            createdAt = history.createdAt
        )

        val savedEntity = jpaRepository.save(entity)

        return PaymentHistory(
            id = savedEntity.id,
            paymentId = savedEntity.paymentId,
            fromStatus = savedEntity.fromStatus,
            toStatus = savedEntity.toStatus,
            createdAt = savedEntity.createdAt
        )
    }
}