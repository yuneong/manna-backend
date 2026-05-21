# Manna Backend 아키텍처 문서

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Kotlin 1.9.25 |
| Framework | Spring Boot 3.5 |
| Auth | Spring Security + JWT (JJWT 0.12.6) |
| ORM | Spring Data JPA |
| DB | MySQL |
| Validation | Jakarta Validation |
| API 문서 | SpringDoc OpenAPI 2.8.6 (Swagger UI) |
| Build | Gradle (Kotlin DSL) |

---

## 아키텍처 원칙

- **DDD + Clean Layered Architecture** — 도메인 우선 패키지 구조
- **Facade 패턴** — Controller는 Facade만 의존, 비즈니스 흐름은 Facade가 조율
- **레이어별 객체 분리** — 레이어 경계마다 전용 객체 사용

```
Request → Controller → Facade → DomainService → Repository (interface)
                                                         ↑
                                              RepositoryImpl (infrastructure)
                                                         ↑
                                                  JpaRepository
```

### 레이어별 객체

| 레이어 | 객체 | 방향 |
|---|---|---|
| interfaces | `Request` / `Response` | HTTP ↔ Application |
| application | `Command` / `Info` | Facade ↔ DomainService |
| domain | `Entity` | 도메인 핵심 모델 |

### 객체 변환 규칙

```
Request.toCommand()     // interfaces → application
Info.from(entity)       // domain → application  
Response.from(info)     // application → interfaces
```

---

## 패키지 구조

```
src/main/kotlin/com/manna/
├── MannaApplication.kt
├── common/
│   ├── auth/
│   │   ├── JwtProperties.kt
│   │   ├── JwtTokenProvider.kt
│   │   └── JwtAuthenticationFilter.kt
│   ├── config/
│   │   └── SecurityConfig.kt
│   └── exception/
│       ├── ErrorCode.kt
│       ├── MannaException.kt
│       └── GlobalExceptionHandler.kt
├── user/
│   ├── domain/
│   │   ├── entity/User.kt
│   │   ├── repository/UserRepository.kt       ← interface (domain boundary)
│   │   └── service/UserDomainService.kt
│   ├── application/
│   │   ├── command/SignUpCommand.kt
│   │   ├── command/LoginCommand.kt
│   │   ├── info/UserInfo.kt
│   │   ├── info/TokenInfo.kt
│   │   └── facade/UserFacade.kt
│   ├── infrastructure/
│   │   ├── jpa/UserJpaRepository.kt
│   │   └── repository/UserRepositoryImpl.kt   ← UserRepository 구현체
│   └── interfaces/
│       ├── controller/UserController.kt
│       └── dto/
│           ├── SignUpRequest.kt
│           ├── LoginRequest.kt
│           ├── TokenResponse.kt
│           └── UserResponse.kt
└── meeting/
    ├── domain/
    │   ├── entity/
    │   │   ├── Meeting.kt
    │   │   ├── MeetingParticipant.kt
    │   │   ├── MeetingSchedule.kt
    │   │   └── MeetingStatus.kt
    │   ├── repository/MeetingRepository.kt    ← interface (domain boundary)
    │   └── service/MeetingDomainService.kt
    ├── application/
    │   ├── command/
    │   │   ├── CreateMeetingCommand.kt
    │   │   ├── JoinMeetingCommand.kt
    │   │   ├── UpdateScheduleCommand.kt
    │   │   └── ConfirmDateCommand.kt
    │   ├── info/
    │   │   ├── MeetingInfo.kt
    │   │   └── ScheduleHeatmapInfo.kt
    │   └── facade/MeetingFacade.kt
    ├── infrastructure/
    │   ├── jpa/MeetingJpaRepository.kt
    │   └── repository/MeetingRepositoryImpl.kt
    └── interfaces/
        ├── controller/MeetingController.kt
        └── dto/
            ├── CreateMeetingRequest.kt
            ├── UpdateScheduleRequest.kt
            ├── ConfirmDateRequest.kt
            ├── MeetingResponse.kt
            ├── MyScheduleResponse.kt
            └── HeatmapResponse.kt
```

---

## Common 레이어

### JWT 인증 흐름

```
클라이언트 요청
    │
    ▼
JwtAuthenticationFilter
    ├── Authorization 헤더에서 Bearer 토큰 추출
    ├── 토큰 없음 → 필터 통과 → Spring Security AuthenticationEntryPoint
    │       └── 401 JSON {"status":401,"message":"로그인이 필요합니다"}
    ├── 토큰 있음 + 유효하지 않음 (만료, 변조 등)
    │       └── 401 JSON {"status":401,"message":"유효하지 않은 토큰입니다"} 후 필터 체인 중단
    └── 토큰 유효 → JwtTokenProvider.getUserId() → SecurityContext에 Authentication 주입
    │
    ▼
Controller (@AuthenticationPrincipal userId: Long)
```

### JwtTokenProvider

```kotlin
generateToken(userId: Long): String   // 토큰 발급
getUserId(token: String): Long        // userId 추출
validateToken(token: String): Boolean // 유효성 검사
```

### SecurityConfig

- `/api/v1/users/sign-up`, `/api/v1/users/login` — 인증 없이 허용
- 나머지 모든 경로 — JWT 인증 필수
- 세션: STATELESS
- CSRF: disabled

### ErrorCode

| 코드 | HTTP | 메시지 |
|---|---|---|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다 |
| `DUPLICATE_EMAIL` | 409 | 이미 사용 중인 이메일입니다 |
| `INVALID_PASSWORD` | 401 | 비밀번호가 일치하지 않습니다 |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다 |
| `MEETING_NOT_FOUND` | 404 | 약속방을 찾을 수 없습니다 |
| `NOT_MEETING_HOST` | 403 | 약속방 방장만 가능한 작업입니다 |
| `ALREADY_JOINED` | 409 | 이미 참여한 약속방입니다 |
| `MEETING_NOT_OPEN` | 400 | 진행 중인 약속방이 아닙니다 |
| `DATE_OUT_OF_RANGE` | 400 | 약속방 날짜 범위를 벗어난 날짜입니다 |

---

## 문서 목록

| 문서 | 내용 |
|---|---|
| [erd.md](erd.md) | 전체 테이블 DDL, 인덱스 설계, 변경 이력 |
| [domain-user.md](domain-user.md) | User 도메인 ERD, API, 레이어 흐름 |
| [domain-meeting.md](domain-meeting.md) | Meeting 도메인 ERD, API, 레이어 흐름 |
| [swagger.md](swagger.md) | Swagger UI 접속 및 JWT 인증 사용법 |
| [frontend-guide.md](frontend-guide.md) | 프론트엔드 API 연동 가이드 |

---

## 에러 응답 형식

모든 에러는 동일한 형식으로 반환됩니다.

```json
{
  "status": 409,
  "message": "이미 사용 중인 이메일입니다"
}
```

---

## 설정 파일 구조

설정 파일은 공통/환경별로 분리되어 있습니다.

```
src/main/resources/
├── application.yml           # 공통 설정 (git 추적 O)
├── application-local.yml     # 로컬 실제 값 (git 추적 X)
├── application-dev.yml       # dev 환경변수 참조 (git 추적 O)
└── application-prod.yml      # prod 환경변수 참조 (git 추적 X)
```

### 프로파일별 동작

| 환경 | ddl-auto | show-sql | 값 관리 |
|---|---|---|---|
| local | `create` | true | yml 파일 직접 기입 |
| dev | `update` | true | 환경변수 |
| prod | `validate` | false | 환경변수 |

> `application.yml`에 `spring.profiles.active: local`이 기본값으로 설정되어 있습니다.

### 공통 설정 (application.yml)

```yaml
spring:
  profiles:
    active: local
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
    open-in-view: false

jwt:
  expiration: 86400000  # 24시간 (ms)
```

### dev / prod 필요 환경변수

```
DB_URL        jdbc:mysql://{host}:3306/manna?...
DB_USERNAME
DB_PASSWORD
JWT_SECRET    (32자 이상)
```

---

## 로컬 실행

### 사전 요구사항

```sql
CREATE DATABASE manna CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### application-local.yml 작성

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/manna?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: {DB_USERNAME}
    password: {DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true

jwt:
  secret: {최소 32자 이상의 시크릿 키}
```

### 실행

```bash
# 로컬 (기본값)
./gradlew bootRun

# 프로파일 지정
./gradlew bootRun --args='--spring.profiles.active=dev'

# 배포 환경
SPRING_PROFILES_ACTIVE=prod java -jar manna.jar
```

---

## 향후 확장 계획

| 항목 | 내용 |
|---|---|
| 카카오 OAuth | `User.kakaoId` 필드 이미 준비됨. `kakao_id`로 로그인 흐름 추가 |
| 장소 결정 | `place` 도메인 추가 (후보 장소 투표) |
| 정산 | `settlement` 도메인 추가 |
| Refresh Token | `TokenInfo`에 `refreshToken` 필드 추가, Redis 저장 |
