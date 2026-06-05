# test_backend

Spring Boot 3.3 + Java 17 기반의 JWT 인증 REST API 백엔드 프로젝트입니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Database | H2 (Embedded, 개발용) |
| ORM | Spring Data JPA |
| Build | Gradle Kotlin DSL |
| Validation | Jakarta Bean Validation |

## 실행 방법

```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

서버 기본 포트: `http://localhost:8080`

## API 엔드포인트

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | /api/auth/register | 없음 | 회원가입 |
| POST | /api/auth/login | 없음 | 로그인 |
| POST | /api/auth/refresh | Refresh Token (Bearer) | 액세스 토큰 갱신 |
| GET | /api/users/me | Bearer JWT | 내 정보 조회 |
| PUT | /api/users/me | Bearer JWT | 내 정보 수정 |
| GET | /api/health | 없음 | 헬스체크 |
| POST | /api/posts | Bearer JWT | 게시글 생성 |
| GET | /api/posts | Bearer JWT | 게시글 목록 조회 |
| GET | /api/posts/{id} | Bearer JWT | 게시글 단건 조회 |
| PUT | /api/posts/{id} | Bearer JWT | 게시글 수정 (본인만) |
| PATCH | /api/posts/{id}/status | Bearer JWT | 상태 변경 (본인만) |
| DELETE | /api/posts/{id} | Bearer JWT | 게시글 삭제 (본인만) |
| POST | /api/todos | 없음 | 할 일 생성 (`created_by` 필수) |
| GET | /api/todos | 없음 | 할 일 목록 조회 (`id ASC`) |
| GET | /api/todos/{id} | 없음 | 할 일 단건 조회 |
| PUT | /api/todos/{id} | 없음 | 할 일 수정 |
| PATCH | /api/todos/{id}/completed | 없음 | 완료 여부 변경 |
| DELETE | /api/todos/{id} | 없음 | 할 일 삭제 |

### 예시 요청

**회원가입**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","name":"홍길동"}'
```

**로그인**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

**내 정보 조회**
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer {accessToken}"
```

**헬스체크**
```bash
curl http://localhost:8080/api/health
# 응답: {"status":"UP","timestamp":"2024-01-01T00:00:00"}
```

**할 일 생성**
```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Todo 구현","created_by":"junior"}'
```

## H2 콘솔

애플리케이션 실행 후 아래 주소로 접근:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (빈칸)

## 패키지 구조

```
com.test.backend
├── config/          SecurityConfig, JwtConfig
├── controller/      AuthController, UserController, HealthController, PostController, TodoController
├── service/         AuthService, UserService, PostService, TodoService
├── repository/      UserRepository, PostRepository, TodoRepository
├── domain/entity/   User, Post, Todo
├── dto/request/     LoginRequest, RegisterRequest, UpdateUserRequest, CreatePostRequest, UpdatePostRequest, UpdatePostStatusRequest, CreateTodoRequest, UpdateTodoRequest, UpdateTodoCompletedRequest
├── dto/response/    TokenResponse, UserResponse, PostResponse, TodoResponse
├── security/        JwtTokenProvider, JwtAuthenticationFilter
└── exception/       GlobalExceptionHandler, ApiException
```

## JWT 설정

`application.yml`에서 아래 항목을 환경에 맞게 수정하세요:

```yaml
jwt:
  secret: your-secret-key-must-be-at-least-256-bits-long-for-hs256
  access-token-expiration: 3600000    # 1시간 (ms)
  refresh-token-expiration: 604800000 # 7일 (ms)
```
