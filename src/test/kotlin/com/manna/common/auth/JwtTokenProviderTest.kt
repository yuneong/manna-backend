package com.manna.common.auth

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        val properties = JwtProperties().apply {
            secret = "test-secret-key-must-be-at-least-32-bytes!!"
            expiration = 3600000L
        }
        jwtTokenProvider = JwtTokenProvider(properties)
    }

    @Nested
    inner class ValidateToken {

        @Test
        fun `유효한 토큰이면 true 반환`() {
            val token = jwtTokenProvider.generateToken(1L)
            assertThat(jwtTokenProvider.validateToken(token)).isTrue()
        }

        @Test
        fun `잘못된 형식의 토큰이면 false 반환 (예외 누출 없음)`() {
            assertThat(jwtTokenProvider.validateToken("invalid.token.value")).isFalse()
        }

        @Test
        fun `만료된 토큰이면 false 반환`() {
            val expiredProperties = JwtProperties().apply {
                secret = "test-secret-key-must-be-at-least-32-bytes!!"
                expiration = -1000L
            }
            val expiredProvider = JwtTokenProvider(expiredProperties)
            val token = expiredProvider.generateToken(1L)
            assertThat(jwtTokenProvider.validateToken(token)).isFalse()
        }

        @Test
        fun `빈 문자열 토큰이면 false 반환`() {
            assertThat(jwtTokenProvider.validateToken("")).isFalse()
        }
    }

    @Nested
    inner class GetUserId {

        @Test
        fun `유효한 토큰에서 userId 추출`() {
            val token = jwtTokenProvider.generateToken(42L)
            assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L)
        }

        @Test
        fun `잘못된 토큰이면 MannaException 발생`() {
            assertThatThrownBy { jwtTokenProvider.getUserId("bad.token") }
                .isInstanceOf(MannaException::class.java)
                .extracting { (it as MannaException).errorCode }
                .isEqualTo(ErrorCode.INVALID_TOKEN)
        }
    }
}