#  <img width="25" height="25" alt="manna-header-48" src="https://github.com/user-attachments/assets/d287dc10-86ec-47f2-aad7-c2f9ee1e65b2" /> manna


> 친구들과 약속 잡는 모든 과정을 한 곳에서 — 날짜 조율부터 장소 결정, 정산까지

<br>

## 📌 프로젝트 소개

친구들과 약속 하나를 잡으려면, 각자 일정을 공유하고 누군가 취합해서 되는 날을 찾고, 장소 후보를 올려서 의견을 모으고, 만남이 끝나면 정산까지 해야 합니다.

**manna**는 이 흐름을 하나의 약속방에서 끝낼 수 있도록 만든 웹 서비스입니다.

<br>

## ✨ 주요 기능

| 단계 | 기능 | 상태 |
|------|------|------|
| 날짜 조율 | 약속방 생성, 참여자 초대, 가용 날짜 선택, 히트맵 결과 조회, 날짜 확정 | ✅ 완료 |
| 장소 결정 | 장소 제안, 좋아요 투표 | ✅ 완료 |
| 정산 | 참여자 선택, 금액 입력, 1/N 정산 | 🚧 개발 중 |

<br>

## 🛠 기술 스택

### Backend
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)

### Frontend
![Vue](https://img.shields.io/badge/Vue_3-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white)
![Pinia](https://img.shields.io/badge/Pinia-FFD859?style=flat-square&logo=pinia&logoColor=black)

### Infra
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonaws&logoColor=white)

<br>

## 🏗 아키텍처

### 백엔드 아키텍처

**DDD + Clean Layered Architecture** 를 기반으로 도메인 중심 설계를 적용했습니다.

```
interfaces (Controller)  →  Request / Response DTO
       ↓
application (Facade)     →  Command / Info 객체
       ↓
domain (Service)         →  Entity, Repository Interface
       ↓
infrastructure           →  JPA Repository 구현체
```

- **Facade 패턴** 적용으로 Controller의 유스케이스 조합 로직 분리
- **레이어별 객체 분리**로 각 계층의 관심사 명확화
- **도메인 우선 패키지 구조**로 응집도 향상

### 패키지 구조

```
src/main/kotlin/com/manna/
├── user/
│   ├── domain/          # Entity, Repository Interface, Domain Service
│   ├── application/     # Facade, Command, Info
│   ├── infrastructure/  # JPA Repository 구현체
│   └── interfaces/      # Controller, Request/Response DTO
├── meeting/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
└── common/
    ├── config/          # Security, JPA 설정
    ├── exception/       # Global Exception Handler
    └── auth/            # JWT Provider
```

<br>

## 📱 화면 소개

### 회원가입 / 로그인

| | |
|---|---|
| ![login](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/login.png) | ![signup](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/signup.png) |
| 로그인 | 회원가입 |

- 이메일·비밀번호(영문+숫자 8자 이상)·닉네임 입력으로 가입. 이메일 중복 시 즉시 안내
- 이메일 저장 옵션 제공. 로그인 후 이전에 접근하려던 페이지로 자동 이동
- 카카오 소셜 로그인 지원

---

### 약속방

| | |
|---|---|
| ![meeting-list-empty](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/meeting-list-empty.png) | ![meeting-list](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/meeting-list.png) |
| 약속 없을 때 | 약속 목록 |

- 내가 참여 중인 약속을 카드 목록으로 확인. 약속이 없으면 만들기 안내 화면 표시

### 약속 만들기

| | |
|---|---|
| ![meeting-create](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/meeting-create.png) | ![meeting-create-filled](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/meeting-create-filled.png) |
| 빈 폼 | 입력 중 |

- 제목(최대 50자), 설명(선택), 날짜 범위를 입력해 약속방 생성
- 필수 항목 미입력 시 제출 차단

### 약속 수정

| | |
|---|---|
| ![meeting-detail](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/meeting-detail.png) | ![meeting-edit-warning](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/meeting-edit-warning.png) |
| 약속 상세 | 날짜 범위 변경 경고 |

- 제목·설명·날짜 범위 수정 가능
- 날짜 범위를 바꾸면 기존 참여자 응답이 초기화된다는 경고 모달 표시

---

### 약속 상세 — 일정 탭

참여자가 각자 가능한 날짜를 입력하고, 방장이 최종 날짜를 확정합니다.

**내 일정**

![schedule-my](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-my.png)

- 캘린더에서 가능한 날짜를 직접 탭/클릭해 선택·저장. 확정 전까지 언제든 수정 가능

**히트맵**

| | |
|---|---|
| ![schedule-heatmap-host](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-heatmap-host.png) | ![schedule-heatmap-member](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-heatmap-member.png) |
| 방장 시점 | 참여자 시점 |

- 날짜별 참여 가능 인원을 4단계 색상으로 시각화
- 셀 클릭 시 해당 날짜의 가능/불가 참여자 목록 표시

**날짜 확정 (방장)**

| | |
|---|---|
| ![schedule-confirm-all](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-confirm-all.png) | ![schedule-confirm-partial](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-confirm-partial.png) |
| 전원 가능 | 일부 불가 |

- 히트맵에서 날짜를 선택하면 우측에 가능 인원 비율과 참여자 명단 표시
- 전원 가능이면 파란 확정 버튼, 일부 불가면 주의 문구와 함께 확정 가능

| | |
|---|---|
| ![schedule-confirmed](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-confirmed.png) | ![schedule-confirm-cancel](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-confirm-cancel.png) |
| 확정 완료 | 확정 취소 |

**동률 — 직접 결정 (방장)**

| | |
|---|---|
| ![schedule-tie-direct](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-tie-direct.png) | ![schedule-tie-direct-confirm](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/schedule-tie-direct-confirm.png) |
| 동률 직접 결정 | 직접 결정 확정 |

**재투표 후보 선정 (방장)**

![revote-select](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/revote-select.png)

- 동률이 난 경우 방장이 2개 이상의 후보 날짜를 골라 재투표 시작
- 전원 가능 / 일부 불가 여부를 색상과 텍스트로 구분

**재투표**

| | | |
|---|---|---|
| ![revote-host](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/revote-host.png) | ![revote-member](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/revote-member.png) | ![revote-in-progress](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/revote-in-progress.png) |
| 방장 결과 화면 | 참여자 투표 화면 | 진행 중 배너 |

| | |
|---|---|
| ![revote-done](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/revote-done.png) | ![revote-cancel](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/revote-cancel.png) |
| 재투표 완료 | 재투표 취소 |

- 참여자: 후보 날짜 중 1개를 선택해 투표. 투표 후에도 변경 가능
- 방장: 참여자와 같은 기능 + 후보별 득표수·투표자 명단·미투표자 목록 실시간 확인. 결과 확정 또는 동률 시 직접 결정

---

### 약속 상세 — 장소 탭

참여자가 장소를 제안하고 마음에 드는 곳에 투표합니다.

| | | |
|---|---|---|
| ![place-empty](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/place-empty.png) | ![place-first](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/place-first.png) | ![place-tie-first](https://raw.githubusercontent.com/yuneong/manna-frontend/main/docs/images/place-tie-first.png) |
| 제안 전 | 단독 1위 | 공동 1위 |

- 장소 이름, 링크(선택), 메모(선택)를 입력해 제안. 누구나 여러 장소를 제안 가능
- 마음에 드는 장소의 하트 버튼을 눌러 투표. 여러 장소에 중복 투표 가능하며 재클릭 시 취소
- 장소 카드는 득표 순으로 정렬. 1위 장소는 강조 테두리와 1위 배지로 표시. 공동 1위면 공동 1위 배지 표시
- 각 카드에 제안자 닉네임, 투표자 아바타, 득표수/전체 인원(예: 4/10명) 표시

---

### 약속 상세 — 정산 탭

🚧 준비 중입니다.

<br>

## 🚀 서비스 바로 이용하기

**AWS EC2 배포 환경에서 직접 사용해볼 수 있습니다.**

👉 **[http://43.200.169.122](http://43.200.169.122)**

### 접속 방법

1. 위 링크 접속
2. **자체 회원가입** 또는 **카카오 로그인**으로 시작
3. 약속방 생성 후 링크를 친구에게 공유

<br>

## 📁 관련 레포지토리

- **Backend**: [manna-backend](https://github.com/yuneong/manna-backend) ← 현재 레포
- **Frontend**: [manna-frontend](https://github.com/yuneong/manna-frontend)

<br>

## 🗓 개발 현황

- **2025.05 ~ 진행 중**
- 개인 사이드 프로젝트
