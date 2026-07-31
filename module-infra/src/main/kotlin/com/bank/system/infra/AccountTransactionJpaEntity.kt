package com.bank.system.infra

import com.bank.system.domain.TransactionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "account_transactions")
class AccountTransactionJpaEntity(
    @Id
    val id: Long,

    @Column(nullable = false, length = 20)
    val accountNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: TransactionType,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val balanceAfter: Long,

    @Column(nullable = true, length = 50)
    val counterpartyName: String? = null,

    @Column(nullable = true, length = 20)
    val counterpartyAccountNumber: String? = null,

    @Column(nullable = true, length = 100)
    val memo: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
