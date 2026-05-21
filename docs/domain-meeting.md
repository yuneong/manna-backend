# Meeting 도메인

## ERD

```
meetings
├── id                BIGINT PK AUTO_INCREMENT
├── host_id           BIGINT NOT NULL (users.id 참조 — FK 없음, 도메인 분리)
├── title             VARCHAR NOT NULL
├── description       VARCHAR (nullable)
├── date_range_start  DATE NOT NULL
├── date_range_end    DATE NOT NULL
├── confirmed_date    DATE (nullable)
├── status            ENUM(OPEN, CONFIRMED, CANCELLED) NOT NULL
└── created_at        DATETIME NOT NULL

meeting_participants
├── id          BIGINT PK AUTO_INCREMENT
├── meeting_id  BIGINT NOT NULL (FK → meetings)
├── user_id     BIGINT NOT NULL
└── joined_at   DATETIME NOT NULL

meeting_schedules
├── id              BIGINT PK AUTO_INCREMENT
├── meeting_id      BIGINT NOT NULL (FK → meetings)
├── user_id         BIGINT NOT NULL
└── scheduled_date  DATE NOT NULL
```

> **도메인 분리 원칙**: `Meeting`은 `User` 엔티티를 직접 참조하지 않고 `hostId: Long`으로 ID만 보관합니다. 크로스 도메인 JPA 조인을 방지합니다.

---

## 패키지 구조

```
meeting/
├── domain/
│   ├── entity/
│   │   ├── Meeting.kt
│   │   ├── MeetingParticipant.kt
│   │   ├── MeetingSchedule.kt
│   │   └── MeetingStatus.kt
│   ├── repository/MeetingRepository.kt         ← 도메인 레포지터리 인터페이스
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

## MeetingStatus

| 상태 | 설명 |
|---|---|
| `OPEN` | 날짜 조율 진행 중 |
| `CONFIRMED` | 날짜 확정 완료 |
| `CANCELLED` | 약속 취소 |

---

## 도메인 규칙

### Meeting 엔티티

| 메서드 | 규칙 |
|---|---|
| `confirmDate(userId, date)` | 방장 여부 → `NOT_MEETING_HOST` / CANCELLED 상태 → `MEETING_NOT_OPEN` / 날짜 범위 → `DATE_OUT_OF_RANGE` (OPEN·CONFIRMED 모두 가능) |
| `cancelConfirm(userId)` | 방장 여부 → `NOT_MEETING_HOST` / CONFIRMED 아니면 → `MEETING_NOT_CONFIRMED` / confirmedDate=null, status=OPEN |
| `requireOpen()` | OPEN이 아니면 `MEETING_NOT_OPEN` |
| `isHost(userId)` | `hostId == userId` |

### MeetingDomainService

| 메서드 | 규칙 |
|---|---|
| `create()` | 약속방 생성 후 방장을 참여자로 자동 등록 |
| `join()` | OPEN 상태 확인, 중복 참여 → `ALREADY_JOINED` |
| `updateSchedule()` | CONFIRMED 상태 → `MEETING_ALREADY_CONFIRMED` / 기존 날짜 전체 삭제 후 신규 등록(replace), 날짜 범위 검증 |
| `confirmDate()` | Meeting 엔티티의 `confirmDate()` 위임 (OPEN·CONFIRMED 모두 허용) |
| `cancelConfirm()` | Meeting 엔티티의 `cancelConfirm()` 위임 |
| `getScheduleHeatmap()` | `{ "날짜": [userId, ...] }` 형태로 집계 (날짜별 가능한 userId 목록) |
| `getMySchedules()` | 특정 미팅에서 본인이 선택한 약속 날짜 목록 반환 |
| `getParticipantCount()` | 약속방 참여자 수 반환 |
| `isParticipant()` | 특정 사용자의 참여 여부 반환 |

---

## API

### POST /api/v1/meetings

약속방 생성 (인증 필요) — 방장이 자동으로 참여자에 추가됨

**Request**
```json
{
  "title": "6월 회식",
  "description": "팀 회식 날짜 잡기",
  "dateRangeStart": "2025-06-01",
  "dateRangeEnd": "2025-06-30"
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "hostId": 1,
  "title": "6월 회식",
  "description": "팀 회식 날짜 잡기",
  "dateRangeStart": "2025-06-01",
  "dateRangeEnd": "2025-06-30",
  "confirmedDate": null,
  "status": "OPEN",
  "createdAt": "2025-05-18T12:00:00",
  "participantCount": 1,
  "responseCount": 0,
  "participants": [
    { "id": 1, "nickname": "홍길동" }
  ]
}
```

---

### GET /api/v1/meetings/{meetingId}

약속방 상세 조회 (인증 필요)

**Response** `200 OK`
```json
{
  "id": 1,
  "hostId": 1,
  "title": "6월 회식",
  "description": "팀 회식 날짜 잡기",
  "dateRangeStart": "2025-06-01",
  "dateRangeEnd": "2025-06-30",
  "confirmedDate": null,
  "status": "OPEN",
  "createdAt": "2025-05-18T12:00:00",
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

> - `isParticipant`: 단건 조회에만 포함됩니다. 목록 조회(`/my`)에는 포함되지 않습니다.
> - `responseCount`: 약속 날짜(schedule)를 1개 이상 등록한 참여자 수 (distinct userId 기준).

---

### GET /api/v1/meetings/my

내 약속방 목록 조회 (인증 필요)

**Response** `200 OK` — `List<MeetingResponse>`

각 항목은 단건 조회와 동일한 구조이며, `isParticipant` 필드는 포함되지 않습니다.

---

### POST /api/v1/meetings/{meetingId}/join

약속방 참여 (인증 필요)

**Response** `200 OK`

**오류**
- `MEETING_NOT_FOUND` — 존재하지 않는 약속방
- `ALREADY_JOINED` — 이미 참여 중
- `MEETING_NOT_OPEN` — OPEN 상태가 아님

---

### GET /api/v1/meetings/{meetingId}/schedules/me

내가 선택한 약속 날짜 조회 (인증 필요)

**Response** `200 OK`
```json
{
  "meetingId": 1,
  "scheduledDates": ["2026-06-10", "2026-06-11", "2026-06-15"]
}
```

**오류**
- `MEETING_NOT_FOUND` — 존재하지 않는 약속방

---

### PUT /api/v1/meetings/{meetingId}/schedules

약속 날짜 등록 (인증 필요) — 기존 날짜를 모두 교체(replace)

**Request**
```json
{
  "scheduledDates": ["2025-06-10", "2025-06-11", "2025-06-15"]
}
```

**Response** `200 OK`

**오류**
- `DATE_OUT_OF_RANGE` — 약속방 날짜 범위를 벗어난 날짜 포함

---

### GET /api/v1/meetings/{meetingId}/heatmap

전체 참여자 가능 날짜 조회 (인증 필요)

**Response** `200 OK`
```json
{
  "meetingId": 1,
  "heatmap": {
    "2025-06-10": {
      "count": 3,
      "availableParticipantIds": [1, 2, 3]
    },
    "2025-06-11": {
      "count": 2,
      "availableParticipantIds": [1, 2]
    },
    "2025-06-15": {
      "count": 4,
      "availableParticipantIds": [1, 2, 3, 4]
    }
  }
}
```

- `count`: 해당 날짜에 가능하다고 표시한 참여자 수
- `availableParticipantIds`: 해당 날짜에 가능한 참여자 ID 배열 (날짜 확정 화면에서 참여 가능/불가 참여자 구분 표시에 사용)

---

### POST /api/v1/meetings/{meetingId}/confirm

날짜 확정 (방장만 가능) — OPEN·CONFIRMED 상태 모두 가능 (재확정 지원)

**Request**
```json
{
  "confirmedDate": "2025-06-15"
}
```

**Response** `200 OK` — MeetingResponse (`status: "CONFIRMED"`, `confirmedDate` 포함, `participants`·`participantCount`·`responseCount` 포함)

**오류**
- `NOT_MEETING_HOST` — 방장이 아닌 사용자가 요청
- `MEETING_NOT_OPEN` — CANCELLED 상태 약속방
- `DATE_OUT_OF_RANGE` — 날짜 범위를 벗어난 날짜

---

### DELETE /api/v1/meetings/{meetingId}/confirm

날짜 확정 취소 (방장만 가능)

**Response** `200 OK` — MeetingResponse (`status: "OPEN"`, `confirmedDate: null`)

**오류**
- `NOT_MEETING_HOST` — 방장이 아닌 사용자가 요청
- `MEETING_NOT_CONFIRMED` — 이미 OPEN 상태인 약속방

---

### PUT /api/v1/meetings/{meetingId}/schedules 추가 제한

**오류**
- `MEETING_ALREADY_CONFIRMED` — CONFIRMED 상태 약속방 (방장·참여자 모두 400)
