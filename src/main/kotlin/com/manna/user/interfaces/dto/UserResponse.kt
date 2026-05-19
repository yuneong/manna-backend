package com.manna.user.interfaces.dto

import com.manna.user.application.info.UserInfo

data class UserResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(info: UserInfo) = UserResponse(
            id = info.id,
            email = info.email,
            nickname = info.nickname,
            profileImageUrl = info.profileImageUrl,
        )
    }
}
