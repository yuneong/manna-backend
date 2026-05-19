# Swagger (OpenAPI 3.0)

## 접속 URL

| 환경 | Swagger UI | API Docs (JSON) |
|---|---|---|
| local | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| dev | https://{dev-host}/swagger-ui.html | https://{dev-host}/v3/api-docs |

> prod 환경에서는 Swagger를 비활성화하는 것을 권장합니다. ([비활성화 방법](#prod-비활성화))

---

## 구성 파일

| 파일 | 역할 |
|---|---|
| `common/config/OpenApiConfig.kt` | OpenAPI 메타정보, JWT 보안 스킴 설정 |
| `common/config/SecurityConfig.kt` | `/swagger-ui/**`, `/v3/api-docs/**` 인증 없이 허용 |
| `application.yml` | springdoc 경로 설정 |

---

## JWT 인증 사용 방법

1. `POST /api/v1/users/login` 으로 로그인
2. 응답의 `accessToken` 값 복사
3. Swagger UI 우측 상단 **Authorize** 버튼 클릭
4. `Bearer Authentication` 항목에 `accessToken` 값 입력 (Bearer 접두사 없이)
5. **Authorize** 클릭 → 이후 모든 요청에 자동으로 헤더 포함

---

## API 그룹

| Tag | 설명 |
|---|---|
| `User` | 회원가입, 로그인, 내 정보 조회, 탈퇴 |
| `Meeting` | 약속방 생성/조회/참여, 가용 날짜, 히트맵, 날짜 확정 |

---

## 어노테이션 사용 규칙

컨트롤러에 아래 어노테이션을 사용합니다.

```kotlin
@Tag(name = "User", description = "사용자 API")          // 클래스 — API 그룹 이름
@Operation(summary = "회원가입")                          // 메서드 — 엔드포인트 요약
@ApiResponse(responseCode = "201", description = "...")  // 메서드 — 응답 설명
@SecurityRequirement(name = "Bearer Authentication")     // 인증 필요 엔드포인트
```

인증이 필요 없는 엔드포인트(`/sign-up`, `/login`)에는 `@SecurityRequirement`를 붙이지 않습니다.

---

## application.yml 설정

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html      # Swagger UI 접속 경로
    tags-sorter: alpha           # 태그 알파벳 정렬
    operations-sorter: alpha     # 엔드포인트 알파벳 정렬
  api-docs:
    path: /v3/api-docs           # OpenAPI JSON 경로
```

---

## prod 비활성화

`application-prod.yml`에 아래 설정을 추가하면 prod 환경에서 Swagger를 완전히 비활성화할 수 있습니다.

```yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```
