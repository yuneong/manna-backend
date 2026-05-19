package com.manna.user.application.info

import com.manna.user.domain.entity.User

data class UserInfo(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(user: User) = UserInfo(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl,
        )
    }
}
