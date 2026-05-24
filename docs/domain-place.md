# Place 도메인

## ERD

```
places
├── id            BIGINT PK AUTO_INCREMENT
├── meeting_id    BIGINT NOT NULL (FK → meetings)
├── suggested_by  BIGINT NOT NULL (FK → users)
├── name          VARCHAR(100) NOT NULL
├── url           VARCHAR(500) NULL
├── memo          VARCHAR(120) NULL
└── created_at    DATETIME NOT NULL

place_votes
├── id        BIGINT PK AUTO_INCREMENT
├── place_id  BIGINT NOT NULL (FK → places)
└── user_id   BIGINT NOT NULL
         UNIQUE (place_id, user_id)
```

---

## 패키지 구조

```
place/
├── domain/
│   ├── entity/
│   │   ├── Place.kt
│   │   └── PlaceVote.kt
│   ├── repository/
│   │   └── PlaceRepository.kt
│   └── service/
│       └── PlaceService.kt
├── application/
│   ├── command/
│   │   └── CreatePlaceCommand.kt
│   ├── info/
│   │   └── PlaceInfo.kt        ← ProposerInfo, PlaceInfo, PlacesInfo
│   └── facade/
│       └── PlaceFacade.kt
├── infrastructure/
│   ├── jpa/
│   │   └── PlaceJpaRepository.kt
│   └── repository/
│       └── PlaceRepositoryImpl.kt
└── interfaces/
    ├── controller/
    │   └── PlaceController.kt
    └── dto/
        ├── CreatePlaceRequest.kt
        └── PlaceResponse.kt    ← ProposerResponse, PlaceResponse, PlacesResponse
```

---

## 도메인 규칙

### PlaceService

| 메서드 | 규칙 |
|---|---|
| `propose()` | MEETING_NOT_FOUND / status가 CONFIRMED·PLACE_VOTING 아님 → `MEETING_NOT_CONFIRMED` / 참여자 아님 → `NOT_MEETING_PARTICIPANT` / status == CONFIRMED이면 제안 후 PLACE_VOTING으로 전환 |
| `toggleVote()` | 참여자 아님 → `NOT_MEETING_PARTICIPANT` / 장소 없음 → `PLACE_NOT_FOUND` / 다른 약속방 장소 → `PLACE_NOT_FOUND` / 기존 투표 있으면 DELETE, 없으면 INSERT |
| `getPlaces()` | 약속방의 모든 장소 반환 |
| `getVotesByPlaceIds()` | 장소 ID 목록의 모든 투표 반환, 빈 리스트면 early return |
| `deleteAllByMeetingId()` | 장소별 votes Hard Delete → 장소 전체 Hard Delete |

### PlaceFacade

| 메서드 | 역할 |
|---|---|
| `getPlaces(meetingId, userId)` | 장소 목록 + 투표 현황 + totalParticipants. 득표 수 내림차순 정렬. myVoted는 요청 userId 기준 |
| `propose(command)` | 장소 제안 후 PlaceInfo 반환 (voteCount=0, myVoted=false) |
| `toggleVote(meetingId, placeId, userId)` | PlaceService.toggleVote 위임 |

---

## API

### GET /api/v1/meetings/{meetingId}/places

장소 목록 조회 — 득표 수 내림차순 정렬 (인증 필요)

**Response** `200 OK`
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

---

### POST /api/v1/meetings/{meetingId}/places

장소 제안 (참여자 전용) — meeting.status = CONFIRMED 필요

**Request**
```json
{
  "name": "와인바 이태원",
  "url": "https://map.naver.com/...",
  "memo": "1인당 4-5만원"
}
```

**Response** `201 Created` — PlaceResponse (voteCount=0, myVoted=false)

**오류**
- `NOT_MEETING_PARTICIPANT` — 참여자가 아닌 사용자 (403)
- `MEETING_NOT_CONFIRMED` — 약속방이 CONFIRMED 상태가 아님 (400)

---

### POST /api/v1/meetings/{meetingId}/places/{placeId}/vote

장소 투표/취소 토글 (참여자 전용)

**Request Body** 없음

**Response** `200 OK`

**오류**
- `NOT_MEETING_PARTICIPANT` — 참여자가 아닌 사용자 (403)
- `PLACE_NOT_FOUND` — 존재하지 않는 장소 (404)
