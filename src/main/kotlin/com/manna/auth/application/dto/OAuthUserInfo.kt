package com.manna.auth.application.dto

import com.manna.common.domain.OAuthProvider
import com.manna.user.application.command.SocialLoginCommand

data class OAuthUserInfo(
    val socialId: String,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
    val provider: OAuthProvider,
) {
    fun toCommand() = SocialLoginCommand(
        provider = provider,
        socialId = socialId,
        email = email,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
    )
}
