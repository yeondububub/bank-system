package com.bank.system.infra

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "accounts")
class AccountJpaEntity(
    @Id
    val id: Long,

    @Column(nullable = false, unique = true)
    val ownerId: Long,

    @Column(nullable = false)
    var balance: Long
)