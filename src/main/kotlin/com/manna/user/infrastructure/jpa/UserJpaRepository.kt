package com.manna.user.infrastructure.jpa

import com.manna.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByEmailAndDeletedAtIsNull(email: String): User?
    fun existsByEmailAndDeletedAtIsNull(email: String): Boolean
    fun findByIdAndDeletedAtIsNull(id: Long): User?
    fun findByIdInAndDeletedAtIsNull(ids: List<Long>): List<User>
    fun findByKakaoIdAndDeletedAtIsNull(kakaoId: String): User?
    fun findByGoogleIdAndDeletedAtIsNull(googleId: String): User?
}
