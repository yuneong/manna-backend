package com.manna

import com.manna.common.auth.JwtProperties
import com.manna.common.config.CorsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class, CorsProperties::class)
class MannaApplication

fun main(args: Array<String>) {
	runApplication<MannaApplication>(*args)
}
