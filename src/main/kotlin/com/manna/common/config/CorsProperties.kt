package com.manna.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
class CorsProperties {
    var allowedOrigins: List<String> = emptyList()
}
