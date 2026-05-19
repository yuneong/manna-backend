package com.manna.user.interfaces.dto

import com.manna.user.application.command.LoginCommand
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,
) {
    fun toCommand() = LoginCommand(email = email, password = password)
}
