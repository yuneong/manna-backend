package com.manna.user.infrastructure.repository

import com.manna.user.domain.entity.User
import com.manna.user.domain.repository.UserRepository
import com.manna.user.infrastructure.jpa.UserJpaRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User =
        userJpaRepository.save(user)

    override fun findById(id: Long): User? =
        userJpaRepository.findByIdAndDeletedAtIsNull(id)

    override fun findByEmail(email: String): User? =
        userJpaRepository.findByEmailAndDeletedAtIsNull(email)

    override fun existsByEmail(email: String): Boolean =
        userJpaRepository.existsByEmailAndDeletedAtIsNull(email)
}
