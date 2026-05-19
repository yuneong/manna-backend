package com.manna.common.auth

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(private val jwtProperties: JwtProperties) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun generateToken(userId: Long): String =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + jwtProperties.expiration))
            .signWith(key)
            .compact()

    fun getUserId(token: String): Long =
        parseClaims(token).subject.toLong()

    fun validateToken(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

    private fun parseClaims(token: String): Claims =
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: JwtException) {
            throw MannaException(ErrorCode.INVALID_TOKEN)
        }
}
