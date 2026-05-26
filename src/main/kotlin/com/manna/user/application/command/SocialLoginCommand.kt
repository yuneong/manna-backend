package com.manna.user.application.command

import com.manna.common.domain.OAuthProvider

data class SocialLoginCommand(
    val provider: OAuthProvider,
    val socialId: String,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
)
