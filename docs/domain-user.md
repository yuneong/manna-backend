# User 도메인

## ERD

```
users
├── id                BIGINT PK AUTO_INCREMENT
├── email             VARCHAR NOT NULL UNIQUE
├── password          VARCHAR NOT NULL (BCrypt 해시)
├── nickname          VARCHAR NOT NULL
├── profile_image_url VARCHAR (nullable)
├── created_at        DATETIME NOT NULL
├── updated_at        DATETIME NOT NULL
└── deleted_at        DATETIME (nullable) ← null이면 활성 계정, 값이 있으면 탈퇴
```

> **소프트 딜리트**: 탈퇴 시 레코드를 삭제하지 않고 `deleted_at`에 시각을 기록합니다.
> 모든 조회 쿼리는 `deleted_at IS NULL` 조건을 포함합니다.
> 카카오 로그인 전환 시 `kakao_id` 컬럼을 마이그레이션으로 추가할 예정입니다.

---

## 패키지 구조

```
user/
├── domain/
│   ├── entity/User.kt                          ← 엔티티
│   ├── repository/UserRepository.kt            ← 도메인 레포지터리 인터페이스
│   └── service/UserDomainService.kt            ← 도메인 서비스
├── application/
│   ├── command/SignUpCommand.kt
│   ├── command/LoginCommand.kt
│   ├── info/UserInfo.kt
│   ├── info/TokenInfo.kt
│   └── facade/UserFacade.kt
├── infrastructure/
│   ├── jpa/UserJpaRepository.kt               ← JPA 레포지터리
│   └── repository/UserRepositoryImpl.kt       ← UserRepository 구현체
└── interfaces/
    ├── controller/UserController.kt
    └── dto/
        ├── SignUpRequest.kt
        ├── LoginRequest.kt
        ├── TokenResponse.kt
        └── UserResponse.kt
```

---

## 도메인 규칙

### User 엔티티

| 메서드 | 설명 |
|---|---|
| `softDelete()` | `deleted_at`, `updated_at`에 현재 시각 기록 |
| `isDeleted` | `deleted_at != null` |
| `@PreUpdate onUpdate()` | 엔티티 변경 시 `updated_at` 자동 갱신 |

### UserDomainService

| 메서드 | 규칙 |
|---|---|
| `register()` | 이메일 중복 시 `DUPLICATE_EMAIL` |
| `login()` | 미존재 이메일 → `USER_NOT_FOUND`, 비밀번호 불일치 → `INVALID_PASSWORD` |
| `withdraw()` | `softDelete()` 호출 후 저장 |

---

## API

### POST /api/v1/users/sign-up

회원가입 (인증 불필요)

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동"
}
```

**Validation**
- `email`: 이메일 형식, 필수
- `password`: 8자 이상, 필수
- `nickname`: 20자 이하, 필수

**Response** `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "profileImageUrl": null
}
```

---

### POST /api/v1/users/login

로그인 (인증 불필요)

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

**오류**
- `USER_NOT_FOUND` — 존재하지 않는 이메일
- `INVALID_PASSWORD` — 비밀번호 불일치

---

### GET /api/v1/users/me

내 정보 조회 (인증 필요)

**Response** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "profileImageUrl": null
}
```

---

### DELETE /api/v1/users/me

회원 탈퇴 (인증 필요) — 소프트 딜리트

**Response** `204 No Content`

---

## 레이어 흐름

### 회원가입

```
SignUpRequest.toCommand()
    │
    ▼
UserFacade.signUp(SignUpCommand)
    │
    ▼
UserDomainService.register(command)
    ├── existsByEmail() → DUPLICATE_EMAIL
    ├── passwordEncoder.encode(password)
    └── userRepository.save(User(...))
    │
    ▼
UserInfo.from(user) → UserResponse.from(info)
```

### 로그인

```
LoginRequest.toCommand()
    │
    ▼
UserFacade.login(LoginCommand)
    │
    ▼
UserDomainService.login(command)
    ├── findByEmail() → USER_NOT_FOUND
    └── passwordEncoder.matches() → INVALID_PASSWORD
    │
    ▼
jwtTokenProvider.generateToken(user.id)
    │
    ▼
TokenInfo → TokenResponse
```
