package com.manna.user.interfaces.dto

import com.manna.user.application.info.TokenInfo

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
) {
    companion object {
        fun from(info: TokenInfo) = TokenResponse(
            accessToken = info.accessToken,
            tokenType = info.tokenType,
        )
    }
}
