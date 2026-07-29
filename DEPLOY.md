# 배포 가이드 (test_backend)

Spring Boot 백엔드를 **Render**(Docker 웹 서비스)에 배포한다. DB는 외부 **Neon Postgres**.

- 라이브: https://test-backend-83yt.onrender.com  (health: `/api/health`)
- 리모트: `origin` = github.com/kdsky88/test_backend, 브랜치 `main`

## 배포 = main에 push (Render 자동 빌드·배포)

```bash
git push origin main
```

- Render가 `Dockerfile`로 이미지 빌드(`./gradlew clean bootJar -x test`) → 배포.
- 부팅 시 **Flyway 마이그레이션이 실 Neon DB에 적용**된다(`V*__*.sql`, Java 마이그레이션 포함). 새 마이그레이션은 실데이터에 처음 도는 것이니 주의.
- `ddl-auto: validate` — 스키마는 Flyway가 만들고 Hibernate는 검증만.

## 배포 전 로컬 확인

```bash
# Render와 동일한 빌드 태스크로 jar 생성 확인
./gradlew bootJar -x test

# 전체 테스트
./gradlew test

# 로컬에서 실서버처럼 띄우기(Docker/Postgres 없이 H2 PostgreSQL 모드)
./gradlew bootRun --args='--spring.datasource.url=jdbc:h2:mem:runlocal;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password='
# 인증: POST /api/auth/register|login (todos/trips는 /api 프리픽스 없음)
```

## 환경변수 (Render 대시보드에서 설정, 레포에 값 저장 X)

`DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD DB_SSLMODE`(Neon은 require), `JWT_SECRET`(generateValue). 자세한 건 `render.yaml`.
