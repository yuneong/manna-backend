package com.manna.auth.application.service

import com.manna.auth.application.dto.OAuthUserInfo
import com.manna.common.config.OAuthProperties
import com.manna.common.domain.OAuthProvider
import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class OAuthService(
    private val oAuthProperties: OAuthProperties,
    restTemplateBuilder: RestTemplateBuilder,
) {
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    fun getKakaoUserInfo(code: String): OAuthUserInfo {
        val accessToken = exchangeToken(
            tokenUri = oAuthProperties.kakao.tokenUri,
            clientId = oAuthProperties.kakao.clientId,
            clientSecret = oAuthProperties.kakao.clientSecret,
            redirectUri = oAuthProperties.kakao.redirectUri,
            code = code,
        )
        return fetchKakaoUserInfo(accessToken)
    }

    fun getGoogleUserInfo(code: String): OAuthUserInfo {
        val accessToken = exchangeToken(
            tokenUri = oAuthProperties.google.tokenUri,
            clientId = oAuthProperties.google.clientId,
            clientSecret = oAuthProperties.google.clientSecret,
            redirectUri = oAuthProperties.google.redirectUri,
            code = code,
        )
        return fetchGoogleUserInfo(accessToken)
    }

    private fun exchangeToken(
        tokenUri: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String,
        code: String,
    ): String {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_FORM_URLENCODED }
        val params = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("redirect_uri", redirectUri)
            add("code", code)
        }
        return try {
            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.postForObject(tokenUri, HttpEntity(params, headers), Map::class.java)
                as? Map<String, Any?> ?: throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)
            response["access_token"] as? String ?: throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)
        } catch (e: RestClientException) {
            throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchKakaoUserInfo(accessToken: String): OAuthUserInfo {
        val headers = HttpHeaders().apply { setBearerAuth(accessToken) }
        return try {
            val body = restTemplate.exchange(
                oAuthProperties.kakao.userInfoUri,
                HttpMethod.GET,
                HttpEntity<Unit>(headers),
                Map::class.java,
            ).body as? Map<String, Any?> ?: throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)

            val id = body["id"]?.toString() ?: throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)
            val account = body["kakao_account"] as? Map<String, Any?>
            val profile = account?.get("profile") as? Map<String, Any?>

            OAuthUserInfo(
                socialId = id,
                email = account?.get("email") as? String,
                nickname = profile?.get("nickname") as? String ?: "카카오 사용자",
                profileImageUrl = profile?.get("profile_image_url") as? String,
                provider = OAuthProvider.KAKAO,
            )
        } catch (e: RestClientException) {
            throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchGoogleUserInfo(accessToken: String): OAuthUserInfo {
        val headers = HttpHeaders().apply { setBearerAuth(accessToken) }
        return try {
            val body = restTemplate.exchange(
                oAuthProperties.google.userInfoUri,
                HttpMethod.GET,
                HttpEntity<Unit>(headers),
                Map::class.java,
            ).body as? Map<String, Any?> ?: throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)

            OAuthUserInfo(
                socialId = body["sub"] as? String ?: throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED),
                email = body["email"] as? String,
                nickname = body["name"] as? String ?: "구글 사용자",
                profileImageUrl = body["picture"] as? String,
                provider = OAuthProvider.GOOGLE,
            )
        } catch (e: RestClientException) {
            throw MannaException(ErrorCode.SOCIAL_LOGIN_FAILED)
        }
    }
}
