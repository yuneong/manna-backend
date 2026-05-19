package com.manna.common.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
class JwtProperties {
    var secret: String = ""
    var expiration: Long = 0
}
