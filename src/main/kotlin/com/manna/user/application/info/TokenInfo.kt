package com.manna.user.application.info

data class TokenInfo(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
