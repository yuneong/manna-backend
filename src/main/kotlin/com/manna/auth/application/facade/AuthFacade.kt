package com.manna.auth.application.facade

import com.manna.auth.application.service.OAuthService
import com.manna.common.auth.JwtTokenProvider
import com.manna.user.application.info.TokenInfo
import com.manna.user.domain.service.UserDomainService
import org.springframework.stereotype.Component

@Component
class AuthFacade(
    private val oAuthService: OAuthService,
    private val userDomainService: UserDomainService,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    fun kakaoLogin(code: String): TokenInfo {
        val userInfo = oAuthService.getKakaoUserInfo(code)
        val user = userDomainService.findOrCreateSocialUser(userInfo.toCommand())
        return TokenInfo(accessToken = jwtTokenProvider.generateToken(user.id))
    }

    fun googleLogin(code: String): TokenInfo {
        val userInfo = oAuthService.getGoogleUserInfo(code)
        val user = userDomainService.findOrCreateSocialUser(userInfo.toCommand())
        return TokenInfo(accessToken = jwtTokenProvider.generateToken(user.id))
    }
}
