package com.manna

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MannaApplication

fun main(args: Array<String>) {
	runApplication<MannaApplication>(*args)
}
