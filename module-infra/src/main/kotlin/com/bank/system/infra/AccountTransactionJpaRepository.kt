package com.bank.system.infra

import org.springframework.data.jpa.repository.JpaRepository

interface AccountTransactionJpaRepository : JpaRepository<AccountTransactionJpaEntity, Long> {
    fun findAllByAccountNumberOrderByCreatedAtDesc(accountNumber: String): List<AccountTransactionJpaEntity>
    fun findAllByAccountNumberInOrderByCreatedAtDesc(accountNumbers: List<String>): List<AccountTransactionJpaEntity>
}
