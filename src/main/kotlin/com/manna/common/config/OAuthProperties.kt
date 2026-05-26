package com.manna.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth")
data class OAuthProperties(
    val kakao: Provider = Provider(),
    val google: Provider = Provider(),
    val frontendRedirectUri: String = "",
) {
    data class Provider(
        val clientId: String = "",
        val clientSecret: String = "",
        val redirectUri: String = "",
        val tokenUri: String = "",
        val userInfoUri: String = "",
    )
}
