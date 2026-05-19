# ADHD를 위한 일상 플래너

> 루틴 · 투두 · 일기를 하나로 통합한 Android 일정 관리 앱

---

## 프로젝트 개요

### 개발 배경

ADHD 사용자는 **"해야 할 것을 아는 것"** 이 아니라 **"아는 것을 실행으로 옮기는 것"** 에 어려움을 겪습니다.

> *"ADHD is a disorder of performance — of doing what you know rather than knowing what to do."*
> — Russell A. Barkley

기존 루틴·할 일 앱들은 분리되어 있어 전환 피로가 크고, 하루를 돌아볼 수 있는 기록 기능도 부족합니다.  
이 앱은 **루틴 + 투두 + 일기를 하나로 통합**하여 인지 부담을 최소화하고 실행력을 높이는 것을 목표로 합니다.

### 팀 구성

| 학번 | 이름 | 역할 |
|------|------|------|
| 20221726 | 전형진 | Frontend |
| 20241937 | 이서영 | Frontend |
| 20221767 | 이세호 | Backend |
| 20222206 | 정현민 | Backend |

---

## 프로젝트 구조

```
databaseteam/
├── frontend/   # Android 앱 (Java, Android Studio)
└── backend/    # REST API 서버 (Node.js, Express, PostgreSQL)
```

> 백엔드 원본 코드: https://github.com/dltpgh6021/planner-backend.git

---

## 주요 기능

### 홈 화면
- 오늘의 루틴 아이템 + 투두를 한 화면에서 통합 표시
- 완료 체크 시 취소선으로 즉각적인 피드백 제공
- 루틴 아이템 시간순 · 투두 가나다순 정렬
- 월간 잔디 캘린더로 루틴 + 투두 수행 현황 시각화

### 루틴 관리
- 루틴 생성 / 수정 / 삭제
- 요일별 반복 스케줄 설정 (월~일 자유 선택)
- 루틴 내 시간별 아이템(`HH:MM 행동`) 추가 및 완료 체크

### AI 만들기 (Gemini 2.5 Flash)
- **루틴 모드**: 자연어 입력 또는 사진 첨부 → AI가 `HH:MM 행동` 형식의 루틴 아이템 생성
  - 요일 언급 시 스케줄 자동 인식 (예: "매주 금요일" → 금요일만 설정)
  - 미리보기 확인 후 "루틴 등록하기" 버튼으로 DB 저장
- **리스트 모드**: 자연어 입력 또는 사진 첨부 → AI가 간결한 할 일 목록 생성
  - 날짜 선택 후 "리스트 등록하기" 버튼으로 해당 날짜 투두로 저장
- 챗봇 형식 UI — 대화 기록 유지, 이미지 첨부 선택 사항

### 투두 리스트
- 날짜별 투두 추가 / 수정 / 삭제 (직접 입력)
- 완료 체크 및 가나다순 정렬

### 일기
- 날짜 선택 후 일기 작성 / 수정 / 삭제
- 해당 날짜의 완료된 루틴·투두 항목 자동 태그 (초록색 텍스트)
- 제목 / 내용 / 날짜 기반 검색

### 마이페이지
- Google 계정 프로필 이미지 · 이메일 자동 연동
- 회원 탈퇴

---

## 기술 스택

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
| DB 드라이버 | node-postgres (pg) |
| 인증 | JWT (jsonwebtoken) |
| Google 인증 | google-auth-library |
| AI | Google Gemini 2.5 Flash (@google/generative-ai) |

---

## 데이터베이스 스키마

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

-- 루틴 아이템 ("HH:MM 행동설명" 형식)
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

-- 일기
CREATE TABLE DIARIES (
    id          SERIAL PRIMARY KEY,
    user_id     UUID NOT NULL,
    content     TEXT,   -- JSON: {title, content, routineTags, listTags}
    target_date DATE
);
```

---

## API 엔드포인트

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
| POST | `/api/routine/from-image` | AI 루틴 미리보기 생성 (DB 저장 없음) |

### 투두
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/todo?target_date=YYYY-MM-DD` | 날짜별 투두 조회 |
| GET | `/api/todo/all` | 전체 투두 조회 |
| GET | `/api/todo/monthly?year=&month=` | 월별 완료 수 조회 |
| POST | `/api/todo` | 투두 추가 |
| PATCH | `/api/todo/:id` | 투두 수정 |
| DELETE | `/api/todo/:id` | 투두 삭제 |
| POST | `/api/todo/from-ai` | AI 리스트 미리보기 생성 (DB 저장 없음) |

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

## 실행 방법

### Backend

```bash
cd backend
npm install
```

`.env.example`을 복사해 `.env` 파일 생성:

```env
DATABASE_URL=postgresql://유저명:비밀번호@호스트:5432/DB이름
JWT_SECRET=your_jwt_secret_key
GOOGLE_CLIENT_ID=your_google_client_id
GEMINI_API_KEY=your_gemini_api_key
```

```bash
node server.js
```

### Frontend

1. Android Studio에서 `frontend/` 폴더 열기
2. `RetrofitClient.java`의 `BASE_URL`을 서버 주소로 변경
   - 에뮬레이터: `http://10.0.2.2:3000/`
   - 실기기: `http://서버IP:3000/`
3. `google-services.json`을 `frontend/app/` 폴더에 배치
4. 빌드 및 실행

---

## 구현 현황

| 기능 | 상태 |
|------|------|
| Google 로그인 연동 | ✅ 완료 |
| 루틴 / 투두 / 일기 CRUD | ✅ 완료 |
| 요일별 루틴 스케줄 설정 | ✅ 완료 |
| 월간 잔디 캘린더 (루틴 + 투두 합산) | ✅ 완료 |
| 일기 작성 시 완료 항목 자동 태그 | ✅ 완료 |
| 마이페이지 Google 계정 연동 | ✅ 완료 |
| AI 루틴 자동 생성 (Gemini) | ✅ 완료 |
| AI 리스트 자동 생성 (Gemini) | ✅ 완료 |
| AI 요일 스케줄 자동 인식 | ✅ 완료 |
