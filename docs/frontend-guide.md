# Manna API 연동 가이드

## 프로젝트 소개

**Manna**는 친구들과 약속 잡는 과정을 하나로 묶는 웹 서비스입니다.

```
날짜 조율 → 장소 결정 → 정산
```

현재 구현된 범위는 **날짜 조율** 단계입니다.

- 약속방을 만들고 친구를 초대
- 각자 가능한 날짜를 선택
- 히트맵으로 겹치는 날짜 확인
- 방장이 최종 날짜 확정

---

## 서버 주소

| 환경 | URL |
|---|---|
| 로컬 | `http://localhost:8080` |
| 개발 | 추후 공유 |

## CORS

프론트엔드 개발 서버(`http://localhost:5173`)에서 백엔드로의 요청이 허용되어 있습니다.

다른 origin이 필요하면 백엔드 `application-local.yml`의 `cors.allowed-origins` 목록에 추가하면 됩니다.

```yaml
cors:
  allowed-origins:
    - http://localhost:5173
    - http://localhost:3000  # 추가 예시
```

---

## 인증 방식

로그인 후 발급받은 `accessToken`을 모든 인증 필요 API의 헤더에 포함합니다.

```
Authorization: Bearer {accessToken}
```

토큰 유효 기간: **24시간**

---

## 공통 에러 응답

모든 에러는 아래 형식으로 반환됩니다.

```json
{
  "status": 401,
  "message": "유효하지 않은 토큰입니다"
}
```

| HTTP 상태 | 상황 |
|---|---|
| `400` | 잘못된 요청 (validation 실패, 날짜 범위 초과 등) |
| `401` | 인증 실패 (토큰 없음, 만료, 비밀번호 불일치) |
| `403` | 권한 없음 (방장 전용 기능을 일반 참여자가 시도) |
| `404` | 리소스 없음 |
| `409` | 중복 (이메일 중복, 이미 참여한 약속방) |

### 401 케이스별 메시지

| 메시지 | 상황 |
|---|---|
| `"로그인이 필요합니다"` | Authorization 헤더 자체가 없는 경우 |
| `"유효하지 않은 토큰입니다"` | 토큰이 만료되었거나 변조된 경우 |
| `"비밀번호가 일치하지 않습니다"` | 로그인 시 비밀번호 불일치 |

**401 응답 처리 권장 방법**: axios interceptor 등으로 401을 감지해 저장된 토큰을 삭제하고 로그인 화면으로 redirect합니다.

```typescript
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## User API

### 회원가입

```
POST /api/v1/users/sign-up
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동"
}
```

| 필드 | 조건 |
|---|---|
| `email` | 이메일 형식, 필수 |
| `password` | 8자 이상, 필수 |
| `nickname` | 20자 이하, 필수 |

**Response** `201`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "profileImageUrl": null
}
```

---

### 로그인

```
POST /api/v1/users/login
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

> `accessToken`을 로컬 스토리지 또는 메모리에 저장해두고 이후 요청 헤더에 포함합니다.

---

### 내 정보 조회 🔒

```
GET /api/v1/users/me
```

**Response** `200`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "profileImageUrl": null
}
```

---

### 회원 탈퇴 🔒

```
DELETE /api/v1/users/me
```

**Response** `204` (본문 없음)

---

## Meeting API

> 🔒 표시된 API는 모두 `Authorization` 헤더 필요

---

### 약속방 생성 🔒

```
POST /api/v1/meetings
```

**Request**
```json
{
  "title": "6월 회식",
  "description": "팀 회식 날짜 잡기",
  "dateRangeStart": "2026-06-01",
  "dateRangeEnd": "2026-06-30"
}
```

| 필드 | 조건 |
|---|---|
| `title` | 50자 이하, 필수 |
| `description` | 선택 |
| `dateRangeStart` | 날짜 형식 `YYYY-MM-DD`, 필수 |
| `dateRangeEnd` | 날짜 형식 `YYYY-MM-DD`, 필수 |

**Response** `201`
```json
{
  "id": 1,
  "hostId": 1,
  "title": "6월 회식",
  "description": "팀 회식 날짜 잡기",
  "dateRangeStart": "2026-06-01",
  "dateRangeEnd": "2026-06-30",
  "confirmedDate": null,
  "status": "OPEN",
  "createdAt": "2026-05-19T12:00:00",
  "participantCount": 1,
  "responseCount": 0,
  "participants": [
    { "id": 1, "nickname": "홍길동" }
  ]
}
```

> 약속방 생성 시 방장은 자동으로 참여자에 포함됩니다.

---

### 약속방 상세 조회 🔒

```
GET /api/v1/meetings/{meetingId}
```

**Response** `200`
```json
{
  "id": 1,
  "hostId": 1,
  "title": "6월 회식",
  "description": "팀 회식 날짜 잡기",
  "dateRangeStart": "2026-06-01",
  "dateRangeEnd": "2026-06-30",
  "confirmedDate": null,
  "status": "OPEN",
  "createdAt": "2026-05-19T12:00:00",
  "participantCount": 3,
  "responseCount": 2,
  "participants": [
    { "id": 1, "nickname": "홍길동" },
    { "id": 2, "nickname": "김철수" },
    { "id": 3, "nickname": "이영희" }
  ],
  "isParticipant": true
}
```

> - `isParticipant`: 요청한 사용자의 참여 여부. 페이지 진입 시 이 값으로 참여 여부를 판별하세요. `false`일 때만 join API를 호출하면 됩니다.
> - `responseCount`: 약속 날짜(schedule)를 1개 이상 등록한 참여자 수. `participantCount`와 함께 사용해 응답률을 표시할 수 있습니다.
> - `participants`: 참여자 id·nickname 배열. 아바타 목록 표시에 사용하세요.

---

### 내 약속방 목록 🔒

```
GET /api/v1/meetings/my
```

**Response** `200`
```json
[
  {
    "id": 1,
    "hostId": 1,
    "title": "6월 회식",
    "description": "팀 회식 날짜 잡기",
    "dateRangeStart": "2026-06-01",
    "dateRangeEnd": "2026-06-30",
    "confirmedDate": null,
    "status": "OPEN",
    "createdAt": "2026-05-19T12:00:00",
    "participantCount": 3,
    "responseCount": 2,
    "participants": [
      { "id": 1, "nickname": "홍길동" },
      { "id": 2, "nickname": "김철수" },
      { "id": 3, "nickname": "이영희" }
    ]
  }
]
```

> `isParticipant` 필드는 목록 응답에 포함되지 않습니다.

---

### 약속방 수정 🔒 (방장 전용)

```
PUT /api/v1/meetings/{meetingId}
```

**Request**
```json
{
  "title": "6월 동아리 회식",
  "description": "설명 (optional)",
  "dateRangeStart": "2026-06-01",
  "dateRangeEnd": "2026-06-14"
}
```

| 필드 | 조건 |
|---|---|
| `title` | 50자 이하, 필수 |
| `description` | 선택 |
| `dateRangeStart` | 날짜 형식 `YYYY-MM-DD`, 필수 |
| `dateRangeEnd` | 날짜 형식 `YYYY-MM-DD`, 필수 |

**Response** `200` — MeetingResponse

> 날짜 범위를 변경하면 기존 참여자들의 스케줄이 모두 초기화되고 약속방 status가 OPEN으로 변경됩니다.

| 에러 코드 | 상황 |
|---|---|
| `403` | 방장이 아닌 사용자 |
| `400` (`REVOTE_IN_PROGRESS`) | 재투표 진행 중 |

---

### 약속방 삭제 🔒 (방장 전용)

```
DELETE /api/v1/meetings/{meetingId}
```

**Request Body** 없음

**Response** `204` (본문 없음)

| 에러 코드 | 상황 |
|---|---|
| `403` | 방장이 아닌 사용자 |

---

### 약속방 참여 🔒

```
POST /api/v1/meetings/{meetingId}/join
```

**Request Body** 없음

**Response** `200`

| 에러 코드 | 상황 |
|---|---|
| `409` | 이미 참여한 약속방 |
| `400` | OPEN 상태가 아닌 약속방 |

---

### 내 약속 날짜 조회 🔒

```
GET /api/v1/meetings/{meetingId}/schedules/me
```

페이지 마운트 시 호출하여 캘린더의 초기 선택 상태를 복원합니다.

**Response** `200`
```json
{
  "meetingId": 1,
  "scheduledDates": ["2026-06-10", "2026-06-11", "2026-06-15"]
}
```

---

### 약속 날짜 등록 🔒

```
PUT /api/v1/meetings/{meetingId}/schedules
```

본인이 참여 가능한 날짜 목록을 전송합니다.
**기존 선택이 통째로 교체**되므로 항상 현재 선택된 날짜 전체를 보내야 합니다.

**Request**
```json
{
  "scheduledDates": ["2026-06-10", "2026-06-11", "2026-06-15"]
}
```

**Response** `200`

> 날짜를 초기화하려면 빈 배열 `[]`을 보내면 됩니다.

---

### 히트맵 조회 🔒

```
GET /api/v1/meetings/{meetingId}/heatmap
```

날짜별로 가능하다고 응답한 참여자 수와 참여자 ID 목록을 반환합니다. 캘린더 UI 색상 강도 표현 및 날짜 확정 화면에서 참여 가능/불가 참여자 아바타 구분에 사용합니다.

**Response** `200`
```json
{
  "meetingId": 1,
  "heatmap": {
    "2026-06-10": {
      "count": 3,
      "availableParticipantIds": [1, 2, 3]
    },
    "2026-06-11": {
      "count": 2,
      "availableParticipantIds": [1, 2]
    },
    "2026-06-15": {
      "count": 4,
      "availableParticipantIds": [1, 2, 3, 4]
    }
  }
}
```

> - 응답에 포함되지 않은 날짜는 가능한 인원이 0명입니다.
> - `availableParticipantIds`와 `/meetings/{id}` 응답의 `participants` 배열을 조합하면, 선택한 날짜의 참여 가능/불가 참여자를 각각 구분할 수 있습니다.

---

### 날짜 확정 🔒 (방장 전용)

```
POST /api/v1/meetings/{meetingId}/confirm
```

OPEN·CONFIRMED 상태 모두 가능합니다. CONFIRMED 상태에서 재호출하면 `confirmedDate`가 새 날짜로 업데이트됩니다.

**Request**
```json
{
  "confirmedDate": "2026-06-15"
}
```

**Response** `200`
```json
{
  "id": 1,
  "confirmedDate": "2026-06-15",
  "status": "CONFIRMED",
  ...
}
```

| 에러 코드 | 상황 |
|---|---|
| `403` | 방장이 아닌 사용자가 요청 |
| `400` | CANCELLED 상태 약속방 |
| `400` | 날짜 범위를 벗어난 날짜 |

---

### 날짜 확정 취소 🔒 (방장 전용)

```
DELETE /api/v1/meetings/{meetingId}/confirm
```

**Request Body** 없음

**Response** `200`
```json
{
  "id": 1,
  "confirmedDate": null,
  "status": "OPEN",
  ...
}
```

| 에러 코드 | 상황 |
|---|---|
| `403` | 방장이 아닌 사용자가 요청 |
| `400` | 이미 OPEN 상태인 약속방 |

---

### 약속 날짜 등록 추가 제한

```
PUT /api/v1/meetings/{meetingId}/schedules
```

| 에러 코드 | 상황 |
|---|---|
| `400` (`MEETING_ALREADY_CONFIRMED`) | CONFIRMED 상태 약속방 — 방장·참여자 모두 등록 불가 |

---

---

## Place API (장소 투표)

> 🔒 모든 장소 API는 `Authorization` 헤더 필요

---

### 장소 목록 조회 🔒

```
GET /api/v1/meetings/{meetingId}/places
```

**Response** `200`
```json
{
  "places": [
    {
      "id": 1,
      "name": "와인바 이태원",
      "url": "https://map.naver.com/...",
      "memo": "1인당 4-5만원",
      "proposer": { "id": 1, "nickname": "민지" },
      "voteCount": 4,
      "voters": ["민지", "지원", "현우", "예린"],
      "myVoted": true
    }
  ],
  "totalParticipants": 6
}
```

| 필드 | 설명 |
|---|---|
| `voters` | 투표한 참여자 닉네임 배열 |
| `myVoted` | 요청한 사용자의 투표 여부 |
| `totalParticipants` | 약속방 전체 참여자 수 |

> 득표 수 내림차순 정렬됩니다.

---

### 장소 제안 🔒 (참여자 전용)

```
POST /api/v1/meetings/{meetingId}/places
```

**사전 조건**: `meeting.status = CONFIRMED`

**Request**
```json
{
  "name": "와인바 이태원",
  "url": "https://map.naver.com/...",
  "memo": "1인당 4-5만원"
}
```

| 필드 | 조건 |
|---|---|
| `name` | 필수 |
| `url` | 선택 |
| `memo` | 선택, 120자 이하 |

**Response** `201`

| 에러 코드 | 상황 |
|---|---|
| `403` | 참여자가 아닌 사용자 |
| `400` (`MEETING_NOT_CONFIRMED`) | 약속방이 CONFIRMED 상태가 아님 |

---

### 장소 투표/취소 🔒 (참여자 전용)

```
POST /api/v1/meetings/{meetingId}/places/{placeId}/vote
```

**Request Body** 없음

**Response** `200`

> 이미 투표했으면 취소, 없으면 추가하는 토글 방식입니다.

| 에러 코드 | 상황 |
|---|---|
| `403` | 참여자가 아닌 사용자 |
| `404` (`PLACE_NOT_FOUND`) | 존재하지 않는 장소 |

---

## Revote API (재투표)

> 🔒 모든 재투표 API는 `Authorization` 헤더 필요

날짜 조율 결과 동률이 발생하거나 재논의가 필요할 때 방장이 재투표를 개설합니다.

---

### 재투표 생성 🔒 (방장 전용)

```
POST /api/v1/meetings/{meetingId}/revote
```

**Request**
```json
{
  "candidateDates": ["2026-06-05", "2026-06-06"]
}
```

| 필드 | 조건 |
|---|---|
| `candidateDates` | 2개 이상, 히트맵에 실제 존재하는 날짜만 허용 |

**Response** `201`
```json
{
  "status": "OPEN",
  "candidates": [
    { "date": "2026-06-05", "count": 0, "voters": [] },
    { "date": "2026-06-06", "count": 0, "voters": [] }
  ],
  "votedCount": 0,
  "totalCount": 6,
  "myVotedDate": null
}
```

| 에러 코드 | 상황 |
|---|---|
| `403` | 방장이 아닌 사용자 |
| `400` (`REVOTE_ALREADY_EXISTS`) | 이미 진행 중인 재투표 있음 |
| `400` (`REVOTE_INVALID_CANDIDATE_DATE`) | 후보 2개 미만, 또는 히트맵에 없는 날짜 포함 |

---

### 재투표 참여 🔒

```
POST /api/v1/meetings/{meetingId}/revote/vote
```

**Request**
```json
{
  "votedDate": "2026-06-05"
}
```

**Response** `200`

> 전원 투표 완료 시 백엔드에서 자동 처리합니다.
> - **단독 1위**: `meeting.status = CONFIRMED`, `meeting.confirmedDate` 업데이트
> - **동률**: 상태 유지 → 방장 confirm 대기 (`revote.status`는 여전히 `OPEN`)
>
> 투표 완료 후 meeting 상태가 바뀌었을 수 있으므로, `GET /api/v1/meetings/{meetingId}`를 재조회하는 것을 권장합니다.

| 에러 코드 | 상황 |
|---|---|
| `403` | 약속방 참여자가 아닌 사용자 |
| `404` | 진행 중인 재투표 없음 |
| `400` | 후보에 없는 날짜 선택 |
| `409` (`REVOTE_ALREADY_VOTED`) | 중복 투표 |

---

### 재투표 현황 조회 🔒

```
GET /api/v1/meetings/{meetingId}/revote
```

**Response** `200`
```json
{
  "status": "OPEN",
  "candidates": [
    {
      "date": "2026-06-05",
      "count": 4,
      "voters": ["민지", "현우", "도현", "예린"]
    },
    {
      "date": "2026-06-06",
      "count": 2,
      "voters": ["수진", "태양"]
    }
  ],
  "votedCount": 6,
  "totalCount": 6,
  "myVotedDate": "2026-06-05"
}
```

| 필드 | 설명 |
|---|---|
| `status` | `OPEN` (진행 중) / `CLOSED` (종료) |
| `candidates[].voters` | 해당 날짜에 투표한 참여자 닉네임 배열 |
| `votedCount` | 투표를 완료한 참여자 수 |
| `totalCount` | 약속방 전체 참여자 수 |
| `myVotedDate` | 요청한 사용자가 선택한 날짜 (`null`이면 미투표) |

> 가장 최근 revote를 반환합니다 (OPEN·CLOSED 무관).

| 에러 코드 | 상황 |
|---|---|
| `404` | 이 약속방에 재투표 이력 없음 |

---

### 재동률 방장 확정 🔒 (방장 전용)

```
POST /api/v1/meetings/{meetingId}/revote/confirm
```

전원 투표 완료 후 동률이 발생한 경우, 방장이 최종 날짜를 직접 선택합니다.

**Request**
```json
{
  "confirmedDate": "2026-06-05"
}
```

**Response** `200`

처리 결과: `revote.status = CLOSED`, `meeting.confirmedDate` 업데이트, `meeting.status = CONFIRMED`

| 에러 코드 | 상황 |
|---|---|
| `403` | 방장이 아닌 사용자 |
| `404` | 진행 중인 재투표 없음 |
| `400` (`REVOTE_NOT_COMPLETED`) | 아직 전원 투표 미완료 |
| `400` (`REVOTE_INVALID_CANDIDATE_DATE`) | 후보 날짜가 아닌 날짜 선택 |

---

## 약속방 status 값

| 값 | 의미 |
|---|---|
| `OPEN` | 날짜 조율 진행 중 |
| `CONFIRMED` | 날짜 확정 완료 |
| `CANCELLED` | 취소 |

---

## API 테스트

Swagger UI에서 직접 테스트해볼 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

1. `POST /api/v1/users/login` 실행
2. 응답의 `accessToken` 복사
3. 우측 상단 **Authorize** 클릭 → 토큰 입력
4. 이후 🔒 API 자유롭게 테스트
