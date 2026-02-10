# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FinVibe User Service - Spring Boot 4 / Java 21 기반 사용자 관리 마이크로서비스. 헥사고날 아키텍처 기반의 모듈형 구조로 설계되었으며, 로컬 인증과 OAuth2(Google) 인증을 지원합니다.

## Development Commands

### Local Development
```bash
# Start local infrastructure (MariaDB, Redis, MongoDB, Kafka)
docker compose -f infra/docker-compose.yml up -d

# Run application (defaults to 'local' profile)
./gradlew bootRun

# Run with specific profile
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### Build & Test
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests InterestStockTest

# Clean build and create executable jar
./gradlew clean bootJar

# Build outputs to: build/libs/finvibe-user-0.0.1-SNAPSHOT.jar
```

### Configuration Profiles
- `local`: 로컬 개발 환경 (application-local.yml)
- `kafka`: Kafka 설정 (application-kafka.yml)
- `oauth`: OAuth2 설정 (application-oauth.yml)
- `prod`: 프로덕션 환경 (application-prod.yml)

## Architecture

### Hexagonal Architecture + Module Structure

프로젝트는 `depth.finvibe.user` 패키지 아래에 세 가지 최상위 구조로 구성:

- **boot/**: 애플리케이션 부트스트랩, Spring 설정, DI 조립
- **shared/**: 전역 공통 코드 (TimeStampedBaseEntity, DomainException, 공통 에러 처리)
- **modules/**: 도메인 바운디드 컨텍스트 (현재 `modules/user`만 존재)

### Module Layer Structure (`modules/user`)

각 모듈은 헥사고날 아키텍처 원칙을 따르며 다음 레이어로 구성:

```
modules/user/
├── domain/           # 순수 비즈니스 로직, 엔티티, 값 객체
│   ├── User.java
│   ├── RefreshToken.java
│   ├── InterestStock.java
│   ├── vo/          # Value Objects (Email, PasswordHash, LoginId, etc.)
│   ├── enums/       # (AuthProvider, UserRole)
│   └── error/       # UserErrorCode (DomainErrorCode 구현)
├── application/      # 유스케이스, 트랜잭션 경계
│   ├── service/     # AuthService, UserService
│   └── port/        # 인터페이스 (의존성 역전)
│       ├── in/      # UserCommandUseCase, AuthCommandUseCase, UserQueryUseCase
│       └── out/     # UserRepository, TokenProvider, MarketClient 등
├── api/             # REST 컨트롤러, 요청/응답 DTO
│   ├── external/    # 외부(프론트엔드) 노출 API
│   └── internal/    # 내부(마이크로서비스간) API
├── infra/           # 기술 구현체
│   ├── persistence/ # JPA 리포지토리 구현
│   ├── security/    # JWT, OAuth2 구현
│   ├── messaging/   # Kafka 프로듀서
│   ├── client/      # 외부 API 클라이언트
│   └── error/       # UserErrorHttpMapper (에러→HTTP 상태 매핑)
└── dto/             # 모듈 간 공유 DTO
```

### Dependency Rules

**계층 간 의존 방향 (중요)**:
- `api` → `application` ✓
- `application` → `domain` ✓
- `infra` → `application` (port 구현을 위해) ✓
- `domain`/`application` → `shared` (공통 계약만) ✓

**금지**:
- `domain` → `application`/`api`/`infra` ✗
- `application` → `infra` (기술 구현 직접 의존 금지) ✗
- 다른 모듈의 `domain` 직접 import ✗

**Port & Adapter 패턴**:
- Application 레이어는 인터페이스(port)만 의존
- Infra 레이어가 port 구현체(adapter) 제공
- 예: `UserRepository` (port) ← `UserRepositoryImpl` (adapter)

## Error Handling Architecture

도메인 순수성을 유지하기 위한 계층화된 에러 처리:

1. **Domain Layer**: 비즈니스 규칙 위반 시 `DomainException(errorCode)` 발생
   - `UserErrorCode` enum이 `DomainErrorCode` 구현
   - HTTP 상태/프레임워크 타입은 domain에 두지 않음

2. **Application Layer**: 예외를 변환하지 않고 그대로 전파
   - 트랜잭션 경계만 관리 (`@Transactional`)

3. **Infra Layer**: `UserErrorHttpMapper`가 에러코드 → HTTP 상태 매핑
   - `supports(code)`: code instanceof UserErrorCode
   - `toStatus(code)`: switch로 상태 코드 결정

4. **Global Handler**: `GlobalExceptionHandler`가 `DomainException` 캐치
   - 등록된 mapper 중 `supports()`가 true인 것 찾아 HTTP 응답 생성
   - `ErrorResponse(code, messageKey)` 포맷으로 반환

**새 에러 추가 시**:
1. `UserErrorCode` enum에 코드 추가
2. Domain/Application에서 `DomainException` 발생
3. `UserErrorHttpMapper`에 상태 코드 매핑 추가

## Authentication & Security

### JWT Authentication
- Access Token: 1시간 유효 (JWT_SECRET 환경변수로 서명)
- Refresh Token: 14일 유효 (Redis에 저장, 로테이션 지원)
- `JwtTokenProvider`: 토큰 생성/검증 구현
- `JwtArgumentResolver`: `@AuthenticatedUser` 파라미터로 인증 정보 주입

### OAuth2 (Google)
- `CustomOAuth2UserService`: OAuth2 사용자 정보 로드
- `OAuth2SuccessHandler`: 로그인 성공 시 임시 토큰 발급 → 회원가입 플로우로 리다이렉트
- `JwtTemporaryTokenProvider`: OAuth 회원가입용 임시 토큰 (10분 유효)

### Security Configuration
- 공개 엔드포인트: `/auth/**`, `/oauth2/**`, `/open-api/**`, `/swagger-ui/**`
- 인증 필요: `/members/**` (일부 제외)
- 내부 API: `/internal/**` (현재는 공개, 추후 IP 제한 권장)

## Coding Conventions

### Naming
- Package: `소문자` (depth.finvibe.user.modules.user.domain)
- Class/Interface: `PascalCase` (UserService, WalletRepository)
- Method/Field: `camelCase` (findByUserId, createdAt)
- Enum Constants: `UPPER_SNAKE_CASE` (INVALID_PASSWORD, USER_NOT_FOUND)
- UseCase 포트: `*CommandUseCase`, `*QueryUseCase`

### Domain Layer
- 비즈니스 규칙은 도메인에 집중
- 값 객체는 불변성 유지 (연산은 새 인스턴스 반환)
- 엔티티는 상태 변경 메서드로 의도 표현 (deposit, withdraw, updateProfile)
- 팩토리 메서드로 생성 의도 드러내기 (User.create(), User.createOAuthUser())
- Lombok: @RequiredArgsConstructor, @Slf4j, @Builder 사용

### Application Layer
- 트랜잭션 경계는 application 서비스에 `@Transactional`
- 입력 검증은 빠르게 실패 (Fail-fast)
- 포트(인터페이스) 기반 의존
- 응답은 DTO로 반환 (도메인 엔티티 직접 노출 금지)

### DTO
- 위치: `modules/{module}/dto`
- 도메인 → DTO 변환은 DTO 내부 정적 팩토리 사용
  - 예: `UserDto.UserResponse.from(user)`
- 이벤트/메시징 DTO는 record 사용 가능
  - 예: `SignUpEvent`, `SignInEvent`

### Testing
- JUnit 5 + AssertJ
- 테스트 네이밍: `동작_성공/실패` (deposit_success, minus_fail_insufficientBalance)
- `@DisplayName`: 한국어로 행위 중심 문장
- given/when/then 주석으로 흐름 명시
- DomainException 발생 시 errorCode까지 검증

## API Structure

### External API (프론트엔드용)
- Base Path: `/auth`, `/members`
- 인증: `Authorization: Bearer <accessToken>` 헤더
- 문서: `docs/API_엔드포인트_정의서.md` 참조
- Swagger UI: `/swagger-ui/index.html` (local 환경)

### Internal API (마이크로서비스간)
- Base Path: `/internal`
- 예: `/internal/members/{userId}/nickname` (사용자 닉네임 조회)
- 현재는 인증 불필요하나 프로덕션에서는 IP 제한 권장

### API 엔드포인트 경로 규칙
- Swagger/OpenAPI 문서: `/open-api/**` 경로 사용
- 내부 API: `/internal/**` 경로는 Swagger 문서에서 제외

## Infrastructure

### Database
- Primary: MariaDB (port 3306, MARIADB_DATABASE=finvibe)
- JPA Entities: `@Entity` 직접 사용 (개발 편의상 허용)
- QueryDSL: 복잡한 쿼리 지원

### Cache
- Redis (port 6379): Refresh Token 저장

### Messaging
- Kafka (port 9092): 이벤트 발행 (SignUpEvent, SignInEvent)
- Producer: `UserKafkaProducer`
- Topic 네이밍: `{domain}.{event}.v1` (예: user.signup.v1)

### Monitoring
- Actuator: Spring Boot Actuator 통합
- Logging: Slf4j + Lombok @Slf4j

## Environment Variables

필수 환경변수 (프로덕션):
- `JWT_SECRET`: JWT 서명 키 (최소 32자)
- `JWT_ISSUER`: JWT 발급자 (기본값: finvibe-user)
- `OAUTH_REDIRECT_URL`: OAuth2 콜백 URL (기본값: http://localhost:3000/oauth/callback)

DB/인프라 설정은 application-{profile}.yml에서 관리.

## Important Notes

- **Null Safety**: 모든 입력값은 빠르게 검증 (Fail-fast)
- **Security**: 절대 credentials/secrets를 코드에 포함하지 말것
- **Commit Convention**: Conventional Commits 형식 (`feat:`, `fix:`, `docs:` 등)
- **Documentation**: API 변경 시 `docs/API_엔드포인트_정의서.md` 업데이트 필수
- **Language**: 코드 주석/문서는 한국어 우선 (AGENTS.md 참조)

## 추가 사항
- 사용자에게 응답할땐 한국어를 사용할것.

## 커밋 컨벤션
Format: `<type>(<module>): <한국어 설명>`

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`. Module: `asset`, `market`, `trade`, `wallet`, etc.

Multiple changes go as bullet points in the body:
```
feat(asset): 자산 등록 API 구현

- 자산 등록을 위한 REST API 엔드포인트 추가
- 자산 등록 서비스 로직 구현
```