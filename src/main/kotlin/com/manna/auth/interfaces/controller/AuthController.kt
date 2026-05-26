package com.manna.auth.interfaces.controller

import com.manna.auth.application.facade.AuthFacade
import com.manna.common.config.OAuthProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "소셜 로그인 API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authFacade: AuthFacade,
    private val oAuthProperties: OAuthProperties,
) {

    @Operation(summary = "카카오 로그인 페이지로 redirect")
    @GetMapping("/kakao")
    fun kakaoLogin(response: HttpServletResponse) {
        val url = "https://kauth.kakao.com/oauth/authorize" +
            "?client_id=${oAuthProperties.kakao.clientId}" +
            "&redirect_uri=${oAuthProperties.kakao.redirectUri}" +
            "&response_type=code"
        response.sendRedirect(url)
    }

    @Operation(summary = "카카오 OAuth 콜백 — JWT 발급 후 프론트엔드로 redirect")
    @GetMapping("/kakao/callback")
    fun kakaoCallback(@RequestParam code: String, response: HttpServletResponse) {
        val tokenInfo = authFacade.kakaoLogin(code)
        response.sendRedirect("${oAuthProperties.frontendRedirectUri}?token=${tokenInfo.accessToken}")
    }

    @Operation(summary = "구글 로그인 페이지로 redirect")
    @GetMapping("/google")
    fun googleLogin(response: HttpServletResponse) {
        val url = "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=${oAuthProperties.google.clientId}" +
            "&redirect_uri=${oAuthProperties.google.redirectUri}" +
            "&response_type=code" +
            "&scope=openid email profile"
        response.sendRedirect(url)
    }

    @Operation(summary = "구글 OAuth 콜백 — JWT 발급 후 프론트엔드로 redirect")
    @GetMapping("/google/callback")
    fun googleCallback(@RequestParam code: String, response: HttpServletResponse) {
        val tokenInfo = authFacade.googleLogin(code)
        response.sendRedirect("${oAuthProperties.frontendRedirectUri}?token=${tokenInfo.accessToken}")
    }
}
