# 배포 가이드 (test_backend)

Java 17 / Spring Boot API를 Render Docker 서비스에 배포한다. 운영 DB는 Neon PostgreSQL이다. 아래 명령은 `test_backend/`에서 실행한다.

- 운영: https://test-backend-83yt.onrender.com (`GET /api/health`)
- 저장소: `github.com/kdsky88/test_backend`, 배포 브랜치: `main`

## 배포 전 검증

```bash
./gradlew test bootJar
```

Docker가 실행 중이어야 PostgreSQL Testcontainers 마이그레이션 테스트까지 실행된다. Docker가 없으면 해당 테스트가 **건너뛰어지므로**, 성공 표시만 보지 말고 `build/reports/tests/test/index.html`의 skipped 항목을 확인한다.

`.github/workflows/ci.yml`은 push/PR마다 Java 17에서 테스트와 JAR 빌드를 실행하고 테스트 보고서를 보관한다. Render의 Docker 빌드는 테스트를 생략하므로 CI 성공을 확인한 후 배포한다. CI 파일 추가만으로 Render 배포가 CI 완료를 기다리지는 않는다. 운영 저장소의 브랜치 보호/Render 배포 조건은 별도로 설정한다.

기본 `application.yml`은 **PostgreSQL**에 연결한다. Docker 없이 로컬 H2로 실행하려면 다음처럼 명시적으로 덮어쓴다.

```bash
./gradlew bootRun --args='--spring.datasource.url=jdbc:h2:mem:runlocal;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password='
```

## 환경변수

Render 대시보드에서 설정하고 실제 값은 커밋하지 않는다. `render.yaml`은 Blueprint 설정이다. 기존 서비스의 값도 직접 확인한다.

| 변수 | 용도 / 설정 |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Neon 접속 정보 |
| `DB_SSLMODE` | 운영은 `require` |
| `JWT_SECRET` | 운영 전용 서명 키. 개발 기본값 사용 금지 |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `900000` ms = 15분 |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Blueprint는 `7776000000` ms = 90일; 앱 기본값은 7일 |
| `GOOGLE_PLACES_KEY` | 서버 Google Places 검색 |
| `RESEND_API_KEY`, `MAIL_FROM` | 재설정 메일 발송 및 발신자 |
| `APP_WEB_URL` | 재설정 링크의 프론트 주소 |
| `TODO_LEGACY_OWNER_EMAIL` | 기존 데이터 소유자 마이그레이션이 필요한 경우만 설정 |

## 이번 변경의 배포 순서

1. 프론트의 계정 전환/401 처리 개선을 먼저 배포한다.
2. Neon 복구 지점 또는 백업을 확보하고 CI 결과를 확인한다.
3. 백엔드 `main`에 push한다: `git push origin main`.
4. Render 부팅 로그에서 Flyway `V9__user_auth_version.sql` 적용과 Hibernate 스키마 검증 성공을 확인한다.
5. health, 로그인, 여행 조회, 토큰 갱신, 비밀번호 변경·재설정을 확인한다.

`V9`는 `users.auth_version BIGINT NOT NULL DEFAULT 0`을 추가한다. 새 토큰에는 버전이 들어간다. **버전 클레임이 없는 기존 토큰·재설정 링크는 버전 0으로 취급되어 그대로 동작한다 — 배포 때 강제 로그아웃은 없다.** 비밀번호 변경/재설정은 버전을 증가시키고 refresh token을 폐기하므로, 구 토큰도 그 시점에 함께 무효가 된다. 재설정 링크는 성공 후 재사용할 수 없다. 미인증 요청은 `401`, 권한 부족은 `403`으로 구분한다.

운영 확인에서는 새 비밀번호 설정 후 이전 access/refresh token이 거부되는지와 같은 재설정 링크의 두 번째 사용이 실패하는지를 확인한다. 경비는 지원 통화, 양수, 최대 `999999999999.99`, 소수점 둘째 자리까지 허용한다.

## 롤백

이미 적용한 Flyway 파일을 수정하거나 삭제하지 않는다. 이 마이그레이션은 컬럼 추가이므로 데이터 삭제가 필요하지 않다. 다만 이전 인증 코드로 되돌리면 세션 폐기 검사가 사라진다. 인증 수정 배포를 우선하며, 불가피한 코드 롤백 시에는 운영 서명 키 교체와 전체 재로그인 영향까지 함께 검토한다.
