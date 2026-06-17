# Settlement 도메인

## ERD

```
settlements
├── id               BIGINT PK AUTO_INCREMENT
├── meeting_id       BIGINT NOT NULL (FK → meetings)
├── creator_id       BIGINT NOT NULL (FK → users) ← 수금자
├── title            VARCHAR(255) NOT NULL
├── type             VARCHAR(20) NOT NULL  -- TOTAL | ITEMIZED
├── total_amount     INT NULL              -- TOTAL 방식만 사용
├── bank_name        VARCHAR(100) NOT NULL
├── account_number   VARCHAR(100) NOT NULL
├── account_holder   VARCHAR(100) NOT NULL
├── status           VARCHAR(20) NOT NULL  -- IN_PROGRESS | COMPLETED
└── created_at       DATETIME NOT NULL

settlement_participants
├── id             BIGINT PK AUTO_INCREMENT
├── settlement_id  BIGINT NOT NULL (FK → settlements)
├── user_id        BIGINT NOT NULL (FK → users) ← 수금 대상자 (생성자 제외)
├── amount         INT NOT NULL             ← 계산된 1인당 금액
└── is_paid        BOOLEAN NOT NULL DEFAULT false

settlement_items                           -- ITEMIZED 방식 전용
├── id             BIGINT PK AUTO_INCREMENT
├── settlement_id  BIGINT NOT NULL (FK → settlements)
├── name           VARCHAR(255) NOT NULL
└── amount         INT NOT NULL

settlement_item_participants
├── id                   BIGINT PK AUTO_INCREMENT
├── settlement_item_id   BIGINT NOT NULL (FK → settlement_items)
└── user_id              BIGINT NOT NULL (FK → users)
```

---

## 패키지 구조

```
settlement/
├── domain/
│   ├── entity/
│   │   ├── Settlement.kt
│   │   ├── SettlementParticipant.kt
│   │   ├── SettlementItem.kt
│   │   ├── SettlementItemParticipant.kt
│   │   ├── SettlementType.kt           -- TOTAL | ITEMIZED
│   │   └── SettlementStatus.kt         -- IN_PROGRESS | COMPLETED
│   ├── repository/
│   │   └── SettlementRepository.kt
│   └── service/
│       └── SettlementService.kt
├── application/
│   ├── command/
│   │   └── CreateSettlementCommand.kt  -- CreateSettlementItemCommand 포함
│   ├── info/
│   │   └── SettlementInfo.kt           -- SettlementCreatorInfo, SettlementParticipantInfo, SettlementItemInfo 포함
│   └── facade/
│       └── SettlementFacade.kt
├── infrastructure/
│   ├── jpa/
│   │   └── SettlementJpaRepository.kt  -- 4개의 JPA 레포지토리
│   └── repository/
│       └── SettlementRepositoryImpl.kt
└── interfaces/
    ├── controller/
    │   └── SettlementController.kt
    └── dto/
        ├── CreateSettlementRequest.kt  -- CreateSettlementItemRequest 포함
        ├── SettlementResponse.kt       -- SettlementCreatorDto, SettlementParticipantDto, SettlementItemDto 포함
        └── SettlementListResponse.kt
```

---

## 도메인 규칙

### SettlementService

| 메서드 | 규칙 |
|---|---|
| `create()` | MEETING_NOT_FOUND / `meeting.status.isSettlementAddable()` 아님(DONE·CANCELLED 등) → `MEETING_SETTLEMENT_NOT_ADDABLE` / 생성자 비참여자 → `NOT_MEETING_PARTICIPANT` / participantUserIds에 비참여자 포함 → `NOT_MEETING_PARTICIPANT` / 생성 후 meeting.status == PLACE_VOTING이면 → SETTLING 전이 (이미 SETTLING이면 변경 없음) |
| `getByMeetingId()` | 비참여자 → `NOT_MEETING_PARTICIPANT` |
| `getById()` | 비참여자 → `NOT_MEETING_PARTICIPANT` / 정산 없음 → `SETTLEMENT_NOT_FOUND` / 다른 약속방 정산 → `SETTLEMENT_NOT_FOUND` |
| `markPaid()` | 정산 대상자 아님 → `SETTLEMENT_NOT_PARTICIPANT` / 대상자면 isPaid = true |
| `complete()` | 정산 없음 → `SETTLEMENT_NOT_FOUND` / 수금자 아님 → `SETTLEMENT_NOT_CREATOR` / 미납부자 존재 → `SETTLEMENT_NOT_ALL_PAID` / 조건 충족 시 status = COMPLETED |

### 금액 계산 규칙

**TOTAL**: `perAmount = floor(totalAmount / participantUserIds.size)`
- 나머지(totalAmount % size)는 생성자 부담, 저장 안 함

**ITEMIZED**: 각 참여자별 `amount = Σ floor(item.amount / item.participantUserIds.size)` (본인이 포함된 항목 합산)
- 항목별 floor 후 나머지는 생성자 부담

### Meeting 상태 전이

```
PLACE_VOTING → SETTLING  (최초 정산 생성 시, 이미 SETTLING이면 변경 없음)
SETTLING     → DONE      (방장이 PATCH /done 호출, 모든 정산 COMPLETED 필요)
```

정산 생성 가능 상태: `PLACE_VOTING`, `SETTLING` (`isSettlementAddable()` 반환 true)

### SettlementFacade

| 메서드 | 역할 |
|---|---|
| `createSettlement()` | 정산 생성 + creator/participant 유저 정보 조회 후 SettlementInfo 반환 |
| `getSettlements()` | N+1 방지를 위해 participants/items 일괄 조회 후 SettlementInfo 목록 반환 |
| `getSettlement()` | 단건 + 유저 정보 enrichment 후 SettlementInfo 반환 |
| `markPaid()` | SettlementService.markPaid 위임 |
| `complete()` | SettlementService.complete 위임 |

---

## API

### GET /api/v1/meetings/{meetingId}/settlements

정산 목록 전체 조회 — 약속 참여자 전용 (인증 필요)

**Response** `200 OK`
```json
{
  "settlements": [
    {
      "settlementId": 1,
      "title": "회식 정산",
      "type": "TOTAL",
      "status": "IN_PROGRESS",
      "totalAmount": 120000,
      "creator": {
        "userId": 1,
        "nickname": "민지",
        "bankName": "카카오뱅크",
        "accountNumber": "1234-5678",
        "accountHolder": "민지"
      },
      "participants": [
        { "userId": 2, "nickname": "지원", "profileImage": null, "amount": 40000, "isPaid": false },
        { "userId": 3, "nickname": "현우", "profileImage": null, "amount": 40000, "isPaid": true }
      ],
      "items": []
    }
  ]
}
```

**오류**
- `NOT_MEETING_PARTICIPANT` — 약속방 참여자가 아님 (403)

---

### GET /api/v1/meetings/{meetingId}/settlements/{settlementId}

정산 단건 조회 (인증 필요)

**Response** `200 OK` — 위 목록 응답의 settlements[0] 단건

**오류**
- `NOT_MEETING_PARTICIPANT` — 403
- `SETTLEMENT_NOT_FOUND` — 404

---

### POST /api/v1/meetings/{meetingId}/settlements

정산 생성 — 약속 참여자 전원 가능 (인증 필요)

**Request (TOTAL)**
```json
{
  "title": "회식 정산",
  "type": "TOTAL",
  "totalAmount": 120000,
  "participantUserIds": [2, 3, 4],
  "bankName": "카카오뱅크",
  "accountNumber": "1234-5678",
  "accountHolder": "민지"
}
```

**Request (ITEMIZED)**
```json
{
  "title": "회식 정산",
  "type": "ITEMIZED",
  "bankName": "카카오뱅크",
  "accountNumber": "1234-5678",
  "accountHolder": "민지",
  "items": [
    { "name": "음식", "amount": 90000, "participantUserIds": [2, 3, 4] },
    { "name": "술", "amount": 60000, "participantUserIds": [2, 3] }
  ]
}
```

**Response** `201 Created` — SettlementResponse

**오류**
- `MEETING_NOT_FOUND` — 404
- `MEETING_SETTLEMENT_NOT_ADDABLE` — DONE·CANCELLED 등 정산 추가 불가 상태 (400)
- `NOT_MEETING_PARTICIPANT` — 생성자 또는 participantUserIds에 비참여자 포함 (403)

---

### PATCH /api/v1/meetings/{meetingId}/settlements/{settlementId}/pay

본인 납부 완료 처리 — 본인만 가능 (인증 필요)

**Response** `200 OK`

**오류**
- `SETTLEMENT_NOT_PARTICIPANT` — 정산 대상자가 아님 (403)

---

### PATCH /api/v1/meetings/{meetingId}/settlements/{settlementId}/complete

정산 완료 처리 — 수금자(생성자)만 가능 (인증 필요)

**Response** `200 OK`

**오류**
- `SETTLEMENT_NOT_FOUND` — 404
- `SETTLEMENT_NOT_CREATOR` — 수금자가 아님 (403)
- `SETTLEMENT_NOT_ALL_PAID` — 미납부자 존재 (400)
