package com.manna.user.interfaces.dto

import com.manna.user.application.command.SignUpCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:Email(message = "유효한 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    val password: String,

    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(max = 20, message = "닉네임은 20자 이하이어야 합니다")
    val nickname: String,
) {
    fun toCommand() = SignUpCommand(
        email = email,
        password = password,
        nickname = nickname,
    )
}
