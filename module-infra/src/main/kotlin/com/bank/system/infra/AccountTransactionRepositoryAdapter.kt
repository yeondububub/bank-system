package com.bank.system.infra

import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.AccountTransactionRepository
import org.springframework.stereotype.Repository

@Repository
class AccountTransactionRepositoryAdapter(
    private val jpaRepository: AccountTransactionJpaRepository,
    private val snowflakeIdGenerator: SnowflakeIdGenerator
) : AccountTransactionRepository {

    override fun save(transaction: AccountTransaction): AccountTransaction {
        val entity = AccountTransactionJpaEntity(
            id = transaction.id ?: snowflakeIdGenerator.nextId(),
            accountNumber = transaction.accountNumber,
            type = transaction.type,
            amount = transaction.amount,
            balanceAfter = transaction.balanceAfter,
            counterpartyName = transaction.counterpartyName,
            counterpartyAccountNumber = transaction.counterpartyAccountNumber,
            memo = transaction.memo,
            createdAt = transaction.createdAt
        )
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findAllByAccountNumberOrderByCreatedAtDesc(accountNumber: String): List<AccountTransaction> {
        return jpaRepository.findAllByAccountNumberOrderByCreatedAtDesc(accountNumber).map { toDomain(it) }
    }

    override fun findAllByAccountNumberInOrderByCreatedAtDesc(accountNumbers: List<String>): List<AccountTransaction> {
        if (accountNumbers.isEmpty()) return emptyList()
        return jpaRepository.findAllByAccountNumberInOrderByCreatedAtDesc(accountNumbers).map { toDomain(it) }
    }

    private fun toDomain(entity: AccountTransactionJpaEntity): AccountTransaction {
        return AccountTransaction(
            id = entity.id,
            accountNumber = entity.accountNumber,
            type = entity.type,
            amount = entity.amount,
            balanceAfter = entity.balanceAfter,
            counterpartyName = entity.counterpartyName,
            counterpartyAccountNumber = entity.counterpartyAccountNumber,
            memo = entity.memo,
            createdAt = entity.createdAt
        )
    }
}
