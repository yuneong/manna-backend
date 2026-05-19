package com.manna.user.application.command

data class SignUpCommand(
    val email: String,
    val password: String,
    val nickname: String,
)
