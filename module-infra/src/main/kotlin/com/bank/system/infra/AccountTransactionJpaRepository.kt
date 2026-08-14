package com.bank.system.infra

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AccountTransactionJpaRepository : JpaRepository<AccountTransactionJpaEntity, Long> {
    fun findAllByAccountNumberOrderByCreatedAtDesc(accountNumber: String): List<AccountTransactionJpaEntity>
    fun findAllByAccountNumberInOrderByCreatedAtDesc(accountNumbers: List<String>): List<AccountTransactionJpaEntity>
    fun findAllByOrderByCreatedAtDesc(): List<AccountTransactionJpaEntity>

    fun findAllByAccountNumberOrderByCreatedAtDesc(accountNumber: String, pageable: Pageable): Page<AccountTransactionJpaEntity>
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AccountTransactionJpaEntity>
}
