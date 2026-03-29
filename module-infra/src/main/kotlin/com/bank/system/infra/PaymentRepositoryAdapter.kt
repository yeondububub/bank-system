package com.bank.system.infra

import com.bank.system.domain.Payment
import com.bank.system.domain.PaymentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryAdapter(
    private val jpaRepository: PaymentJpaRepository
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        val entity = PaymentJpaEntity(
            id = payment.id,
            orderId = payment.orderId,
            buyerId = payment.buyerId,
            amount = payment.amount,
            status = payment.status
        )

        val savedEntity = jpaRepository.save(entity)

        return toDomain(savedEntity)
    }

    override fun findById(id: Long): Payment? {
        val entity = jpaRepository.findByIdOrNull(id) ?: return null
        return toDomain(entity)
    }

    override fun findByOrderId(orderId: String): Payment? {
        val entity = jpaRepository.findByOrderId(orderId) ?: return null
        return toDomain(entity)
    }

    private fun toDomain(entity: PaymentJpaEntity): Payment {
        return Payment(
            id = entity.id,
            orderId = entity.orderId,
            buyerId = entity.buyerId,
            amount = entity.amount,
            status = entity.status
        )
    }
}
