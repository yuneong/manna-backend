package com.manna.common.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class JwtAuthenticationFilterTest {

    private val jwtTokenProvider: JwtTokenProvider = mock()
    private lateinit var filter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        filter = JwtAuthenticationFilter(jwtTokenProvider)
    }

    @Nested
    inner class DoFilterInternal {

        @Test
        fun `토큰 없으면 필터 체인 정상 진행`() {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain: FilterChain = mock()

            filter.doFilter(request, response, chain)

            verify(chain).doFilter(request, response)
        }

        @Test
        fun `유효한 토큰이면 필터 체인 정상 진행`() {
            val request = MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer valid-token")
            }
            val response = MockHttpServletResponse()
            val chain: FilterChain = mock()

            whenever(jwtTokenProvider.validateToken("valid-token")).thenReturn(true)
            whenever(jwtTokenProvider.getUserId("valid-token")).thenReturn(1L)

            filter.doFilter(request, response, chain)

            verify(chain).doFilter(request, response)
        }

        @Test
        fun `유효하지 않은 토큰이면 401 반환 후 필터 체인 중단`() {
            val request = MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer invalid-token")
            }
            val response = MockHttpServletResponse()
            val chain: FilterChain = mock()

            whenever(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false)

            filter.doFilter(request, response, chain)

            assert(response.status == HttpServletResponse.SC_UNAUTHORIZED)
            assert(response.contentType?.contains("application/json") == true)
            verify(chain, never()).doFilter(request, response)
        }
    }
}