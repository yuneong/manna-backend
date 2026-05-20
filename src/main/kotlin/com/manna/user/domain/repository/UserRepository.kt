package com.manna.user.domain.repository

import com.manna.user.domain.entity.User

interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun findAllByIds(ids: List<Long>): List<User>
}
