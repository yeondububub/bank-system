package com.bank.system.infra

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface AccountJpaRepository : JpaRepository<AccountJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.ownerId = :ownerId")
    fun findByOwnerIdWithLock(ownerId: Long): AccountJpaEntity?
}