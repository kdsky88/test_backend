# P의 여행 플래너 — 백엔드 (Spring Boot)

**P의 여행 플래너** 앱의 REST API 백엔드. JWT 인증, 여행/일정/경비 관리, 그리고 장소 검색·메일 발송을
외부 API로 프록시한다. Render(Docker) + Neon Postgres에 배포.

> 🤖 이 프로젝트는 **AI 코딩 에이전트(Claude Code)와 대화하며** 만든 실배포 백엔드다.

- **라이브**: https://test-backend-83yt.onrender.com (헬스체크 `/api/health`)
- **프론트 레포**: https://github.com/kdsky88/test_front

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| DB | Neon Postgres (운영) · H2 (로컬) |
| ORM / 마이그레이션 | Spring Data JPA · Flyway (V1~V8) |
| 캐시 | Spring Cache (장소 추천 결과) |
| 외부 연동 | Google Places API(New) · Resend(메일) — `RestClient` 프록시, 키는 env only |
| Build / 배포 | Gradle Kotlin DSL · Docker · Render |

**아키텍처**: Flutter 클라이언트 ↔ 이 백엔드 ↔ Neon Postgres.
Google Places·Resend는 **백엔드가 서버 키로 프록시**(키 미노출), 지도·날씨·환율·위키백과는 클라이언트가 직접 호출.

---

## 실행 방법

```bash
# 로컬(H2) 실행 — 별도 DB 없이 뜬다
./gradlew bootRun

# 빌드 / 테스트
./gradlew bootJar -x test
./gradlew test
```

기본 포트: `http://localhost:8080`

### 환경 변수 (운영)

| 변수 | 설명 |
|------|------|
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` `DB_PASSWORD` `DB_SSLMODE` | Postgres 접속 (미설정 시 로컬 H2) |
| `JWT_SECRET` `JWT_ACCESS_TOKEN_EXPIRATION` `JWT_REFRESH_TOKEN_EXPIRATION` | JWT 서명·만료 |
| `GOOGLE_PLACES_KEY` | 장소 검색(서버 전용 키). 미설정 시 `/places/*` → 503 |
| `RESEND_API_KEY` `MAIL_FROM` | 비밀번호 재설정 메일(HTTP API). 미설정 시 발송 대신 로그 |
| `APP_WEB_URL` | 재설정 링크가 가리킬 프론트 웹 주소 |
| `PORT` | 서버 포트 (Render가 주입) |

---

## API 엔드포인트

인증 계열만 `/api` 프리픽스, 도메인 리소스(trips/todos/places)는 프리픽스 없음.

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/auth/register` · `/login` · `/refresh` | 없음 | 가입 · 로그인 · 토큰 갱신 |
| POST | `/api/auth/password` | Bearer | 비밀번호 변경 |
| POST | `/api/auth/forgot` · `/reset` | 없음 | 재설정 링크 메일 · 재설정 |
| GET/PUT | `/api/users/me` | Bearer | 내 정보 조회·수정 |
| GET | `/api/health` | 없음 | 헬스체크 |
| GET/POST | `/trips` | Bearer | 여행 목록·생성 |
| GET/PATCH/DELETE | `/trips/{id}` | Bearer | 여행 조회·수정·삭제 |
| GET | `/trips/{id}/todos` | Bearer | 여행의 일정 목록 |
| GET/POST | `/trips/{id}/expenses` | Bearer | 경비 목록·추가 |
| DELETE | `/trips/{id}/expenses/{id}` | Bearer | 경비 삭제 |
| GET/POST | `/todos` | Bearer | 일정 목록·생성 (`/calendar` `/stats` `/completed` `/tags` `/assignees` 포함) |
| PATCH/DELETE | `/todos/{id}` | Bearer | 일정 수정·삭제 (+ `/{id}/tags`) |
| GET | `/places/recommend` · `/nearby` | Bearer | 관광지·맛집 추천 · 내 주변 (Google Places 프록시) |

## DB 마이그레이션 (Flyway)

`src/main/resources/db/migration` — `V1` 베이스라인 → `V4` posts 제거 → `V6` trips → `V7` todos 위경도/장소명 → `V8` 경비.
`ddl-auto=validate`이므로 새 테이블은 로컬 H2 부팅으로 스키마를 먼저 검증한다.

## 배포

`git push origin main` → Render 자동 빌드·배포(Docker). Flyway가 부팅 시 실 Neon DB에 마이그레이션 적용.
상세는 **[`DEPLOY.md`](DEPLOY.md)**.
