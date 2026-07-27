package com.bank.system.infra

import com.bank.system.domain.AccountStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "accounts")
class AccountJpaEntity(
    @Id
    val id: Long,

    @Column(nullable = false, unique = true)
    val ownerId: Long,

    @Column(nullable = false, unique = true, length = 20)
    val accountNumber: String,

    @Column(nullable = false)
    var balance: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AccountStatus = AccountStatus.PENDING
)