# ERD & DDL

## 테이블 관계도

```
users
  │
  │  (host_id — FK 없음, 도메인 분리)
  │
meetings ──────────────── meeting_participants
    │                           │
    │                     user_id (FK 없음)
    │
    └──────────────────── meeting_schedules
                                │
                          user_id (FK 없음)
```

> `host_id`, `user_id`는 물리적 FK를 걸지 않습니다.
> User 도메인과 Meeting 도메인을 분리하여 크로스 도메인 조인을 방지하는 설계 원칙을 따릅니다.
> 참조 무결성은 애플리케이션 레이어에서 보장합니다.

---

## DDL

### users

```sql
CREATE TABLE users
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255) NOT NULL,
    password          VARCHAR(255) NOT NULL,
    nickname          VARCHAR(20)  NOT NULL,
    profile_image_url VARCHAR(500),
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    deleted_at        DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
```

### meetings

```sql
CREATE TABLE meetings
(
    id               BIGINT                            NOT NULL AUTO_INCREMENT,
    host_id          BIGINT                            NOT NULL,
    title            VARCHAR(50)                       NOT NULL,
    description      VARCHAR(500),
    date_range_start DATE                              NOT NULL,
    date_range_end   DATE                              NOT NULL,
    confirmed_date   DATE,
    status           ENUM ('OPEN','CONFIRMED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    created_at       DATETIME(6)                       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_meetings_host_id (host_id),
    INDEX idx_meetings_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
```

### meeting_participants

```sql
CREATE TABLE meeting_participants
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    joined_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_meeting_participants (meeting_id, user_id),
    CONSTRAINT fk_participants_meeting
        FOREIGN KEY (meeting_id) REFERENCES meetings (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
```

### meeting_schedules

```sql
CREATE TABLE meeting_schedules
(
    id             BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id     BIGINT NOT NULL,
    user_id        BIGINT NOT NULL,
    scheduled_date DATE   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_meeting_schedules (meeting_id, user_id, scheduled_date),
    INDEX idx_meeting_schedules_meeting_id (meeting_id),
    CONSTRAINT fk_meeting_schedules_meeting
        FOREIGN KEY (meeting_id) REFERENCES meetings (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
```

---

## 인덱스 설계

| 테이블 | 인덱스 | 용도 |
|---|---|---|
| `users` | `uq_users_email` (UNIQUE) | 이메일 중복 체크, 로그인 조회 |
| `meetings` | `idx_meetings_host_id` | 방장 기준 약속방 조회 |
| `meetings` | `idx_meetings_status` | 상태 필터링 |
| `meeting_participants` | `uq_meeting_participants` (UNIQUE) | 중복 참여 방지 |
| `meeting_schedules` | `uq_meeting_schedules` (UNIQUE) | 날짜 중복 등록 방지 |
| `meeting_schedules` | `idx_meeting_schedules_meeting_id` | 약속방 기준 약속 날짜 조회 |

---

## 컬럼 설계 참고

| 컬럼 | 타입 결정 이유 |
|---|---|
| `password` VARCHAR(255) | BCrypt 해시 결과는 60자이나 여유 확보 |
| `nickname` VARCHAR(20) | Request Validation 상한과 일치 |
| `title` VARCHAR(50) | Request Validation 상한과 일치 |
| `status` ENUM | 애플리케이션과 DB 모두에서 값 제한 |
| `DATETIME(6)` | 마이크로초 정밀도 — Java `LocalDateTime` 기본 매핑 |

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v1.0.0 | 2026-05-19 | 최초 작성 — users, meetings, meeting_participants, availability |
| v1.1.0 | 2026-05-21 | availability → meeting_schedules 테이블 rename, available_date → scheduled_date |
