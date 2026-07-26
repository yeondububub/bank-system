package com.bank.system.domain

import com.bank.system.domain.exception.DuplicateEmailException

class UserService(
    private val userRepository: UserRepository
) {
    fun signUp(user: User): User {
        if (userRepository.existsByEmail(user.email)) {
            throw DuplicateEmailException(user.email)
        }
        return userRepository.save(user)
    }

    fun getByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun getById(id: Long): User? {
        return userRepository.findById(id)
    }
}
