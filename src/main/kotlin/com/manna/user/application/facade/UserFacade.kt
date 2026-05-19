package com.manna.user.application.facade

import com.manna.common.auth.JwtTokenProvider
import com.manna.user.application.command.LoginCommand
import com.manna.user.application.command.SignUpCommand
import com.manna.user.application.info.TokenInfo
import com.manna.user.application.info.UserInfo
import com.manna.user.domain.service.UserDomainService
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userDomainService: UserDomainService,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    fun signUp(command: SignUpCommand): UserInfo {
        val user = userDomainService.register(command)
        return UserInfo.from(user)
    }

    fun login(command: LoginCommand): TokenInfo {
        val user = userDomainService.login(command)
        return TokenInfo(accessToken = jwtTokenProvider.generateToken(user.id))
    }

    fun getMyInfo(userId: Long): UserInfo {
        val user = userDomainService.getById(userId)
        return UserInfo.from(user)
    }

    fun withdraw(userId: Long) {
        userDomainService.withdraw(userId)
    }
}
