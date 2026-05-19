package com.manna.user.application.command

data class LoginCommand(
    val email: String,
    val password: String,
)
