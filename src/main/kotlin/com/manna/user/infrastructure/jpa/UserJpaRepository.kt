package com.manna.user.infrastructure.jpa

import com.manna.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long> {
    // 탈퇴하지 않은 사용자만 조회
    fun findByEmailAndDeletedAtIsNull(email: String): User?
    fun existsByEmailAndDeletedAtIsNull(email: String): Boolean
    fun findByIdAndDeletedAtIsNull(id: Long): User?
}
