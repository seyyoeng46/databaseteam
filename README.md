# 📓 일상 플래너 앱 (Daily Planner)

> 루틴 관리 · 투두 리스트 · 일기를 하나로 통합한 Android 플래너 앱

---

## 📌 프로젝트 개요

Google 계정으로 로그인하여 **루틴**, **투두 리스트**, **일기**를 관리하는 Android 앱입니다.  
완료한 루틴·투두 항목이 일기 작성 시 자동으로 태그로 연동되어 하루를 기록할 수 있습니다.

---

## 🗂️ 프로젝트 구조

```
databaseteam/
├── frontend/          # Android 앱 (Java, Android Studio)
└── planner-backend/   # REST API 서버 (Node.js, Express, PostgreSQL)
```

---

## ✨ 주요 기능

### 🏠 홈 화면
- 오늘 날짜의 루틴 아이템 목록 표시 (시간 순 정렬)
- 오늘의 투두 리스트 표시 (가나다 순 정렬)
- 완료 체크 시 취소선 표시 (루틴·리스트 동일 굵기 통일)

### 🔄 루틴 관리
- 루틴 생성 / 수정 / 삭제
- 요일별 반복 스케줄 설정
- 루틴 내 시간별 아이템 추가 및 완료 체크

### ✅ 투두 리스트
- 날짜별 투두 추가 / 수정 / 삭제
- 완료 체크 및 목록 가나다 순 정렬
- 월별 달력 뷰에서 투두 현황 확인

### 📖 일기
- 날짜 선택 후 일기 작성 / 수정 / 삭제
- **완료된 루틴·투두 자동 태그**: 일기 작성 시 해당 날짜의 완료 항목이 자동으로 로드
  - 루틴 완료 항목 → 첫 번째 줄 (초록색 텍스트)
  - 리스트 완료 항목 → 두 번째 줄 (초록색 텍스트)
- 제목 / 내용 / 날짜 기반 검색
- 날짜 필터 검색

### 👤 마이페이지
- Google 계정 프로필 이미지 자동 연동
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
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES USERS(id),
    routine_name TEXT NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 루틴 요일 스케줄 (0=일 ~ 6=토)
CREATE TABLE routine_schedules (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    routine_id UUID NOT NULL REFERENCES ROUTINES(id),
    day_of_week INTEGER NOT NULL
);

-- 루틴 아이템 (알람)
CREATE TABLE routine_items (
    id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    routine_id UUID NOT NULL REFERENCES ROUTINES(id),
    title      TEXT NOT NULL
);

-- 루틴 아이템 완료 기록
CREATE TABLE routine_completions (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    item_id     UUID NOT NULL REFERENCES routine_items(id),
    completed_date DATE NOT NULL,
    is_completed BOOLEAN DEFAULT false
);

-- 투두
CREATE TABLE TODOS (
    id          SERIAL PRIMARY KEY,
    user_id     UUID NOT NULL,
    title       TEXT NOT NULL,
    is_completed BOOLEAN DEFAULT false,
    target_date DATE
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
| POST | `/api/auth/google` | Google ID Token으로 로그인/회원가입 |

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
| POST | `/api/routine/:id/items/:itemId/complete` | 완료 체크/해제 |
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
cd planner-backend
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

## 👥 팀원 및 브랜치

| 브랜치 | 담당 |
|--------|------|
| `main` | 통합 브랜치 |
| `hyungjin` | 홈·마이페이지·일기 기능 |

---

## 📝 주요 구현 사항 (hyungjin 브랜치)

- **일기 DB 연동**: 로컬 저장 → REST API 연동으로 전환
- **일기 태그 자동 로드**: 해당 날짜의 완료된 루틴·투두를 병렬 비동기로 로드
- **태그 분리 표시**: 루틴 완료 항목 / 리스트 완료 항목을 분리하여 2줄로 표시
- **날짜 timezone 처리**: `TO_CHAR(target_date, 'YYYY-MM-DD')`로 KST 하루 오차 제거
- **구글 계정 연동**: 프로필 이미지(Glide CircleCrop) 및 이메일 마이페이지 표시
- **홈 화면 정렬**: 루틴 시간순·투두 가나다순 정렬 통일
