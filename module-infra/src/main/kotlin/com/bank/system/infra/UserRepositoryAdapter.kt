package com.bank.system.infra

import com.bank.system.domain.User
import com.bank.system.domain.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository
) : UserRepository {

    override fun save(user: User): User {
        val entity = UserJpaEntity(
            id = user.id,
            email = user.email,
            password = user.password,
            name = user.name,
            createdAt = user.createdAt
        )
        val saved = jpaRepository.save(entity)

        return User(
            id = saved.id,
            email = saved.email,
            password = saved.password,
            name = saved.name,
            createdAt = saved.createdAt
        )
    }

    override fun findByEmail(email: String): User? {
        val entity = jpaRepository.findByEmail(email) ?: return null
        return User(
            id = entity.id,
            email = entity.email,
            password = entity.password,
            name = entity.name,
            createdAt = entity.createdAt
        )
    }

    override fun findById(id: Long): User? {
        val entity = jpaRepository.findById(id).orElse(null) ?: return null
        return User(
            id = entity.id,
            email = entity.email,
            password = entity.password,
            name = entity.name,
            createdAt = entity.createdAt
        )
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaRepository.existsByEmail(email)
    }
}
