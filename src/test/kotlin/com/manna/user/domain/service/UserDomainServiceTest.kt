package com.manna.user.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.user.application.command.LoginCommand
import com.manna.user.application.command.SignUpCommand
import com.manna.user.domain.entity.User
import com.manna.user.domain.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder

class UserDomainServiceTest {

    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private lateinit var userDomainService: UserDomainService

    @BeforeEach
    fun setUp() {
        userDomainService = UserDomainService(userRepository, passwordEncoder)
    }

    private fun user(
        id: Long = 1L,
        email: String = "test@example.com",
        password: String = "encoded_password",
        nickname: String = "홍길동",
    ) = User(id = id, email = email, password = password, nickname = nickname)

    @Nested
    inner class Register {

        @Test
        fun `회원가입 성공`() {
            val command = SignUpCommand("test@example.com", "password123", "홍길동")
            val saved = user(email = command.email, nickname = command.nickname)

            whenever(userRepository.existsByEmail(command.email)).thenReturn(false)
            whenever(passwordEncoder.encode(command.password)).thenReturn("encoded_password")
            whenever(userRepository.save(any())).thenReturn(saved)

            val result = userDomainService.register(command)

            assertThat(result.email).isEqualTo(command.email)
            assertThat(result.nickname).isEqualTo(command.nickname)
            assertThat(result.password).isEqualTo("encoded_password")
        }

        @Test
        fun `이메일 중복 시 DUPLICATE_EMAIL 예외`() {
            val command = SignUpCommand("dup@example.com", "password123", "홍길동")

            whenever(userRepository.existsByEmail(command.email)).thenReturn(true)

            val ex = assertThrows<MannaException> { userDomainService.register(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.DUPLICATE_EMAIL)
        }
    }

    @Nested
    inner class Login {

        @Test
        fun `로그인 성공`() {
            val command = LoginCommand("test@example.com", "password123")
            val found = user(email = command.email)

            whenever(userRepository.findByEmail(command.email)).thenReturn(found)
            whenever(passwordEncoder.matches(command.password, found.password)).thenReturn(true)

            val result = userDomainService.login(command)

            assertThat(result.email).isEqualTo(command.email)
        }

        @Test
        fun `존재하지 않는 이메일 시 USER_NOT_FOUND 예외`() {
            val command = LoginCommand("none@example.com", "password123")

            whenever(userRepository.findByEmail(command.email)).thenReturn(null)

            val ex = assertThrows<MannaException> { userDomainService.login(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.USER_NOT_FOUND)
        }

        @Test
        fun `비밀번호 불일치 시 INVALID_PASSWORD 예외`() {
            val command = LoginCommand("test@example.com", "wrong_password")
            val found = user(email = command.email)

            whenever(userRepository.findByEmail(command.email)).thenReturn(found)
            whenever(passwordEncoder.matches(command.password, found.password)).thenReturn(false)

            val ex = assertThrows<MannaException> { userDomainService.login(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.INVALID_PASSWORD)
        }
    }

    @Nested
    inner class Withdraw {

        @Test
        fun `탈퇴 성공 시 deleted_at 기록되고 저장`() {
            val found = user()

            whenever(userRepository.findById(found.id)).thenReturn(found)
            whenever(userRepository.save(any())).thenReturn(found)

            userDomainService.withdraw(found.id)

            assertThat(found.deletedAt).isNotNull()
            verify(userRepository).save(found)
        }

        @Test
        fun `존재하지 않는 사용자 탈퇴 시 USER_NOT_FOUND 예외`() {
            whenever(userRepository.findById(999L)).thenReturn(null)

            val ex = assertThrows<MannaException> { userDomainService.withdraw(999L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.USER_NOT_FOUND)
        }
    }
}
