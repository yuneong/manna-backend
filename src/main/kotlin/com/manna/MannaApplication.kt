package com.manna

import com.manna.common.auth.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class)
class MannaApplication

fun main(args: Array<String>) {
	runApplication<MannaApplication>(*args)
}
