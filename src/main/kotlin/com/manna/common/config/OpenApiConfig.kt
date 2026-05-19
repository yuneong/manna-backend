package com.manna.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Manna API")
                .description("친구들과 약속 잡는 서비스 — 날짜 조율 · 장소 결정 · 정산")
                .version("v1.0.0"),
        )
        .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            Components().addSecuritySchemes(
                SECURITY_SCHEME_NAME,
                SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("로그인 후 발급받은 accessToken을 입력하세요 (Bearer 접두사 없이)"),
            ),
        )

    companion object {
        const val SECURITY_SCHEME_NAME = "Bearer Authentication"
    }
}
