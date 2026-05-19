package com.manna.user.application.facade

import com.manna.common.auth.JwtTokenProvider
import com.manna.user.application.command.LoginCommand
import com.manna.user.application.command.SignUpCommand
import com.manna.user.domain.entity.User
import com.manna.user.domain.service.UserDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UserFacadeTest {

    private val userDomainService: UserDomainService = mock()
    private val jwtTokenProvider: JwtTokenProvider = mock()
    private lateinit var userFacade: UserFacade

    @BeforeEach
    fun setUp() {
        userFacade = UserFacade(userDomainService, jwtTokenProvider)
    }

    private fun user(id: Long = 1L) =
        User(id = id, email = "test@example.com", password = "encoded", nickname = "홍길동")

    @Nested
    inner class SignUp {

        @Test
        fun `회원가입 성공 시 UserInfo 반환`() {
            val command = SignUpCommand("test@example.com", "password123", "홍길동")
            val saved = user()

            whenever(userDomainService.register(command)).thenReturn(saved)

            val result = userFacade.signUp(command)

            assertThat(result.email).isEqualTo(saved.email)
            assertThat(result.nickname).isEqualTo(saved.nickname)
        }
    }

    @Nested
    inner class Login {

        @Test
        fun `로그인 성공 시 TokenInfo 반환`() {
            val command = LoginCommand("test@example.com", "password123")
            val found = user()
            val token = "jwt.access.token"

            whenever(userDomainService.login(command)).thenReturn(found)
            whenever(jwtTokenProvider.generateToken(found.id)).thenReturn(token)

            val result = userFacade.login(command)

            assertThat(result.accessToken).isEqualTo(token)
            assertThat(result.tokenType).isEqualTo("Bearer")
        }
    }

    @Nested
    inner class GetMyInfo {

        @Test
        fun `내 정보 조회 성공 시 UserInfo 반환`() {
            val found = user()

            whenever(userDomainService.getById(found.id)).thenReturn(found)

            val result = userFacade.getMyInfo(found.id)

            assertThat(result.id).isEqualTo(found.id)
            assertThat(result.email).isEqualTo(found.email)
            assertThat(result.nickname).isEqualTo(found.nickname)
        }
    }

    @Nested
    inner class Withdraw {

        @Test
        fun `탈퇴 요청 시 DomainService에 위임`() {
            userFacade.withdraw(1L)

            verify(userDomainService).withdraw(1L)
        }
    }
}
