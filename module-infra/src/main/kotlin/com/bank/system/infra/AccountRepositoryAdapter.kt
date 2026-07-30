package com.bank.system.infra

import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import com.bank.system.domain.AccountStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AccountRepositoryAdapter(
    private val jpaRepository: AccountJpaRepository
): AccountRepository {

    override fun save(account: Account): Account {
        val entity = AccountJpaEntity(
            id = requireNotNull(account.id) { "Account ID는 필수입니다." },
            ownerId = account.ownerId,
            accountNumber = account.accountNumber,
            balance = account.balance,
            status = account.status,
            isPrimary = account.isPrimary
        )
        val savedEntity = jpaRepository.save(entity)

        return toDomain(savedEntity)
    }

    override fun findById(id: Long): Account? {
        val entity = jpaRepository.findByIdOrNull(id) ?: return null
        return toDomain(entity)
    }

    override fun findByIdWithLock(id: Long): Account? {
        val entity = jpaRepository.findByIdWithLock(id) ?: return null
        return toDomain(entity)
    }

    override fun findByOwnerId(ownerId: Long): Account? {
        val entity = jpaRepository.findByOwnerId(ownerId) ?: return null
        return toDomain(entity)
    }

    override fun findAllByOwnerId(ownerId: Long): List<Account> {
        return jpaRepository.findAllByOwnerId(ownerId).map { toDomain(it) }
    }

    override fun findPrimaryByOwnerId(ownerId: Long): Account? {
        val entity = jpaRepository.findByOwnerIdAndIsPrimaryTrue(ownerId) ?: return null
        return toDomain(entity)
    }

    override fun findByOwnerIdWithLock(ownerId: Long): Account? {
        val entity = jpaRepository.findByOwnerIdWithLock(ownerId) ?: return null
        return toDomain(entity)
    }

    override fun findByAccountNumber(accountNumber: String): Account? {
        val entity = jpaRepository.findByAccountNumber(accountNumber) ?: return null
        return toDomain(entity)
    }

    override fun existsByAccountNumber(accountNumber: String): Boolean {
        return jpaRepository.existsByAccountNumber(accountNumber)
    }

    override fun findPendingAccounts(): List<Account> {
        return jpaRepository.findByStatus(AccountStatus.PENDING).map { toDomain(it) }
    }

    private fun toDomain(entity: AccountJpaEntity): Account {
        return Account(
            id = entity.id,
            ownerId = entity.ownerId,
            accountNumber = entity.accountNumber,
            balance = entity.balance,
            status = entity.status,
            isPrimary = entity.isPrimary
        )
    }
}