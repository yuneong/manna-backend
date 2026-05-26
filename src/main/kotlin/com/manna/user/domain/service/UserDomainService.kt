package com.manna.user.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.common.domain.OAuthProvider
import com.manna.user.application.command.LoginCommand
import com.manna.user.application.command.SignUpCommand
import com.manna.user.application.command.SocialLoginCommand
import com.manna.user.domain.entity.User
import com.manna.user.domain.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserDomainService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun register(command: SignUpCommand): User {
        if (userRepository.existsByEmail(command.email)) {
            throw MannaException(ErrorCode.DUPLICATE_EMAIL)
        }
        return userRepository.save(
            User(
                email = command.email,
                password = passwordEncoder.encode(command.password),
                nickname = command.nickname,
            ),
        )
    }

    fun login(command: LoginCommand): User {
        val user = userRepository.findByEmail(command.email)
            ?: throw MannaException(ErrorCode.USER_NOT_FOUND)
        if (!passwordEncoder.matches(command.password, user.password)) {
            throw MannaException(ErrorCode.INVALID_PASSWORD)
        }
        return user
    }

    fun getById(id: Long): User =
        userRepository.findById(id) ?: throw MannaException(ErrorCode.USER_NOT_FOUND)

    fun getByEmail(email: String): User =
        userRepository.findByEmail(email) ?: throw MannaException(ErrorCode.USER_NOT_FOUND)

    fun getUsersByIds(ids: List<Long>): List<User> =
        userRepository.findAllByIds(ids)

    @Transactional
    fun withdraw(id: Long) {
        val user = getById(id)
        user.softDelete()
        userRepository.save(user)
    }

    @Transactional
    fun findOrCreateSocialUser(command: SocialLoginCommand): User {
        userRepository.findBySocialId(command.provider, command.socialId)?.let { return it }

        val email = command.email?.takeIf { !userRepository.existsByEmail(it) }
            ?: "${command.provider.name.lowercase()}_${command.socialId}@manna.social"

        return userRepository.save(
            User(
                email = email,
                password = passwordEncoder.encode(UUID.randomUUID().toString()),
                nickname = command.nickname,
                profileImageUrl = command.profileImageUrl,
                provider = command.provider,
                kakaoId = if (command.provider == OAuthProvider.KAKAO) command.socialId else null,
                googleId = if (command.provider == OAuthProvider.GOOGLE) command.socialId else null,
            ),
        )
    }
}
