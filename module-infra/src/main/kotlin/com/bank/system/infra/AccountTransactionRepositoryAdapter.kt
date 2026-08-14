package com.bank.system.infra

import com.bank.system.common.util.SnowflakeIdGenerator
import com.bank.system.domain.AccountTransaction
import com.bank.system.domain.AccountTransactionRepository
import com.bank.system.domain.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

    override fun findAllOrderByCreatedAtDesc(): List<AccountTransaction> {
        return jpaRepository.findAllByOrderByCreatedAtDesc().map { toDomain(it) }
    }

    override fun findAllPaged(page: Int, size: Int, accountNumber: String?): PageResult<AccountTransaction> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pagedResult = if (!accountNumber.isNullOrBlank()) {
            val cleanNum = accountNumber.replace("-", "").trim()
            jpaRepository.findAllByAccountNumberOrderByCreatedAtDesc(cleanNum, pageable)
        } else {
            jpaRepository.findAllByOrderByCreatedAtDesc(pageable)
        }

        return PageResult(
            content = pagedResult.content.map { toDomain(it) },
            page = pagedResult.number,
            size = pagedResult.size,
            totalElements = pagedResult.totalElements,
            totalPages = pagedResult.totalPages,
            isLast = pagedResult.isLast
        )
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
