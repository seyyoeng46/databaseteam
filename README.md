# 📓 ADHD를 위한 일상 플래너

> 루틴 · 투두 · 일기를 하나로 통합한 Android 일정 관리 앱

---

## 📌 프로젝트 개요

### 개발 배경

ADHD 사용자는 **"해야 할 것을 아는 것"** 이 아니라 **"아는 것을 실행으로 옮기는 것"** 에 어려움을 겪습니다.

> *"ADHD is a disorder of performance — of doing what you know rather than knowing what to do."*
> — Russell A. Barkley

기존 루틴·할 일 앱들은 앱이 분리되어 있어 전환 피로가 크고, 하루를 돌아볼 수 있는 기록 기능도 부족합니다.  
이 앱은 **루틴 + 투두 + 일기를 하나로 통합**하여 인지 부담을 최소화하고 실행력을 높이는 것을 목표로 합니다.

### 팀 구성

| 학번 | 이름 |
|------|------|
| 20221726 | 전형진 |
| 20241937 | 이서영 |
| 20221767 | 이세호 |
| 20222206 | 정현민 |

---

## 🗂️ 프로젝트 구조

```
databaseteam/
├── frontend/          # Android 앱 (Java, Android Studio)
└── backend/   # REST API 서버 (Node.js, Express, PostgreSQL)
```

---

## ✨ 주요 기능

### 🏠 홈 화면
- 오늘의 루틴 아이템 + 투두를 한 화면에서 통합 표시
- 완료 체크 시 취소선으로 즉각적인 피드백 제공
- 루틴 아이템 시간순 · 투두 가나다순 정렬

### 🔄 루틴 관리
- 루틴 생성 / 수정 / 삭제
- 요일별 반복 스케줄 설정 (월~일 자유 선택)
- 루틴 내 시간별 아이템 추가 및 완료 체크
- 🤖 **AI 루틴 자동 생성** (개발 진행 중)

### ✅ 투두 리스트
- 날짜별 투두 추가 / 수정 / 삭제
- 완료 체크 및 가나다순 정렬
- 월간 달력 뷰에서 루틴 + 투두 수행 현황 통합 확인

### 📖 일기
- 날짜 선택 후 일기 작성 / 수정 / 삭제
- **완료된 루틴·투두 자동 태그**: 해당 날짜의 완료 항목이 자동으로 로드
  - 루틴 완료 항목 → 첫 번째 줄 (초록색 텍스트)
  - 투두 완료 항목 → 두 번째 줄 (초록색 텍스트)
- 제목 / 내용 / 날짜 기반 검색

### 👤 마이페이지
- Google 계정 프로필 이미지 자동 연동 (CircleCrop)
- Google 계정 이메일 표시
- 회원 탈퇴

---

## 🛠️ 기술 스택

### Frontend (Android)
| 항목 | 내용 |
|------|------|
| Language | Java |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |
| HTTP 통신 | Retrofit2 + Gson |
| 이미지 로딩 | Glide 4.16 |
| 인증 | Google Sign-In SDK |

### Backend
| 항목 | 내용 |
|------|------|
| Runtime | Node.js |
| Framework | Express 5 |
| Database | PostgreSQL |
| ORM | node-postgres (pg) |
| 인증 | JWT (jsonwebtoken) |
| Google 인증 | google-auth-library |

---

## 🗄️ 데이터베이스 스키마

```sql
-- 사용자
CREATE TABLE USERS (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    google_id   TEXT UNIQUE NOT NULL,
    email       TEXT,
    username    TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 루틴
CREATE TABLE ROUTINES (
    id           UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES USERS(id),
    routine_name TEXT NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP DEFAULT NOW()
);

-- 루틴 요일 스케줄 (0=일 ~ 6=토)
CREATE TABLE routine_schedules (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    routine_id  UUID NOT NULL REFERENCES ROUTINES(id),
    day_of_week INTEGER NOT NULL
);

-- 루틴 아이템
CREATE TABLE routine_items (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    routine_id UUID NOT NULL REFERENCES ROUTINES(id),
    title      TEXT NOT NULL
);

-- 루틴 아이템 완료 기록
CREATE TABLE routine_completions (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    routine_item_id UUID NOT NULL REFERENCES routine_items(id),
    user_id         UUID NOT NULL REFERENCES USERS(id),
    completed_date  DATE NOT NULL,
    UNIQUE(routine_item_id, completed_date)
);

-- 투두
CREATE TABLE TODOS (
    id           SERIAL PRIMARY KEY,
    user_id      UUID NOT NULL,
    title        TEXT NOT NULL,
    content      TEXT,
    is_completed BOOLEAN DEFAULT false,
    target_date  DATE
);

-- 일기 (title·content·routineTags·listTags를 JSON으로 content 컬럼에 저장)
CREATE TABLE DIARIES (
    id          SERIAL PRIMARY KEY,
    user_id     UUID NOT NULL,
    content     TEXT,       -- JSON packed: {title, content, routineTags, listTags}
    target_date DATE
);
```

---

## 🔌 API 엔드포인트

모든 API는 `Authorization: Bearer <JWT>` 헤더 필요 (`/api/auth` 제외)

### 인증
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/google` | Google ID Token으로 로그인 / 회원가입 |

### 루틴
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/routine` | 루틴 목록 조회 |
| POST | `/api/routine` | 루틴 생성 |
| PATCH | `/api/routine/:id` | 루틴 수정 |
| DELETE | `/api/routine/:id` | 루틴 삭제 |
| GET | `/api/routine/:id/items` | 루틴 아이템 목록 |
| POST | `/api/routine/:id/items` | 아이템 추가 |
| PATCH | `/api/routine/:id/items/:itemId` | 아이템 수정 |
| DELETE | `/api/routine/:id/items/:itemId` | 아이템 삭제 |
| POST | `/api/routine/:id/items/:itemId/complete` | 완료 체크 / 해제 |
| GET | `/api/routine/:id/items/completions` | 날짜별 완료 현황 |

### 투두
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/todo?target_date=YYYY-MM-DD` | 날짜별 투두 조회 |
| GET | `/api/todo/all` | 전체 투두 조회 |
| GET | `/api/todo/monthly?year=&month=` | 월별 투두 조회 |
| POST | `/api/todo` | 투두 추가 |
| PATCH | `/api/todo/:id` | 투두 수정 |
| DELETE | `/api/todo/:id` | 투두 삭제 |

### 일기
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/diary/all` | 전체 일기 목록 |
| GET | `/api/diary?target_date=YYYY-MM-DD` | 날짜별 일기 조회 |
| GET | `/api/diary/search?keyword=` | 키워드 검색 |
| POST | `/api/diary` | 일기 작성 |
| PATCH | `/api/diary/:id` | 일기 수정 |
| DELETE | `/api/diary/:id` | 일기 삭제 |

### 사용자
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/user/me` | 내 정보 조회 |
| PATCH | `/api/user/me` | 닉네임 수정 |
| DELETE | `/api/user/me` | 회원 탈퇴 |

---

## 🚀 실행 방법

### Backend

```bash
cd backend
npm install
```

`.env` 파일 생성:
```env
DATABASE_URL=your_postgresql_connection_string
JWT_SECRET=your_jwt_secret
GOOGLE_CLIENT_ID=your_google_client_id
PORT=3000
```

```bash
node server.js
```

### Frontend

1. Android Studio에서 `frontend/` 폴더 열기
2. `RetrofitClient.java`의 `BASE_URL`을 서버 주소로 변경
   - 에뮬레이터 사용 시: `http://10.0.2.2:3000/`
   - 실기기 사용 시: `http://서버IP:3000/`
3. `google-services.json` 배치 (`frontend/app/` 폴더)
4. 빌드 및 실행

---

## 📋 기획 대비 구현 현황

| 기능 | 상태 |
|------|------|
| 구글 로그인 실제 연동 | ✅ 완료 |
| 루틴 / 투두 / 일기 DB 연동 | ✅ 완료 |
| 요일별 루틴 스케줄 설정 | ✅ 완료 |
| 월간 수행 캘린더 (루틴 + 투두 합산) | ✅ 완료 |
| 일기 작성 시 완료 항목 자동 태그 | ✅ 완료 |
| 마이페이지 구글 계정 연동 | ✅ 완료 |
| AI 루틴 자동 생성 | 🔧 개발 진행 중 |

