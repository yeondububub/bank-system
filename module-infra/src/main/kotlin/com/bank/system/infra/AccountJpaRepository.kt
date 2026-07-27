package com.bank.system.infra

import com.bank.system.domain.AccountStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface AccountJpaRepository : JpaRepository<AccountJpaEntity, Long> {

    fun findByOwnerId(ownerId: Long): AccountJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.ownerId = :ownerId")
    fun findByOwnerIdWithLock(ownerId: Long): AccountJpaEntity?

    fun findByAccountNumber(accountNumber: String): AccountJpaEntity?

    fun existsByAccountNumber(accountNumber: String): Boolean

    fun findByStatus(status: AccountStatus): List<AccountJpaEntity>
}