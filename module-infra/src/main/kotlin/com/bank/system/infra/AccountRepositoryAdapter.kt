package com.bank.system.infra

import com.bank.system.domain.Account
import com.bank.system.domain.AccountRepository
import org.springframework.stereotype.Repository

@Repository
class AccountRepositoryAdapter(
    private val jpaRepository: AccountJpaRepository
) : AccountRepository {

    override fun save(account: Account): Account {
        val entity = AccountJpaEntity(
            id = account.id,
            ownerId = account.ownerId,
            balance = account.balance
        )
        val savedEntity = jpaRepository.save(entity)

        return Account(
            id = savedEntity.id,
            ownerId = savedEntity.ownerId,
            balance = savedEntity.balance
        )
    }

    override fun findByOwnerIdWithLock(ownerId: Long): Account? {
        val entity = jpaRepository.findByOwnerIdWithLock(ownerId) ?: return null

        return Account(
            id = entity.id,
            ownerId = entity.ownerId,
            balance = entity.balance
        )
    }
}