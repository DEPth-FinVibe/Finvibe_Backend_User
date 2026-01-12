# External API 명세 (FinVibe User)

## 개요

이 문서는 FinVibe **User 서비스**의 외부 노출 API 명세를 기술합니다. (프론트엔드 개발자용)

## Base URL

- `https://finvibe.space/api/user`

## 공통 규칙

### 인증

- 인증이 필요한 API는 헤더에 아래 값을 포함해야 합니다.
  - `Authorization: Bearer <accessToken>`
- `Authorization` 누락/형식 불일치 시 `401 Unauthorized`가 발생할 수 있습니다.

### Content-Type

- 요청 바디가 있는 경우 `Content-Type: application/json`
- 응답은 기본적으로 JSON (일부는 본문 없음)

### 에러 응답 포맷

```json
{
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "잘못된 요청입니다.",
  "fieldErrors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

- `fieldErrors`는 유효성 검증 에러 등에서만 포함될 수 있습니다.

---

## 1. 인증 (Auth)

### 로그인

`POST /auth/login`

- 설명: 로그인 ID/비밀번호로 로그인 후 토큰을 발급합니다.
- 인증: 불필요

요청 바디

| 필드명 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `loginId` | String | 로그인 ID | Y |
| `password` | String | 비밀번호 | Y |

요청 예시

```json
{
  "loginId": "user1234",
  "password": "pw1234!"
}
```

응답 (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `accessToken` | String | 액세스 토큰(JWT) |
| `accessExpiresAt` | String(ISO-8601) | 액세스 토큰 만료 시각(UTC) |
| `refreshToken` | String | 리프레시 토큰(JWT) |
| `refreshExpiresAt` | String(ISO-8601) | 리프레시 토큰 만료 시각(UTC) |

응답 예시

```json
{
  "accessToken": "string",
  "accessExpiresAt": "2026-01-01T00:00:00Z",
  "refreshToken": "string",
  "refreshExpiresAt": "2026-01-15T00:00:00Z"
}
```

---

### 회원가입 (로컬/소셜 통합)

`POST /auth/signup`

- 설명: 회원가입 후 토큰을 발급합니다.
  - `temporaryToken`이 있으면 **소셜(OAuth) 가입 완료**로 처리합니다.
- 인증: 불필요

요청 바디

| 필드명 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `loginId` | String | 로그인 ID (규칙: `^[a-zA-Z0-9]{4,20}$`) | Y |
| `password` | String | 비밀번호 | Y |
| `email` | String | 이메일(형식 체크) | Y |
| `birthDate` | String(`YYYY-MM-DD`) | 생년월일(과거 날짜) | Y |
| `phoneNumber` | String | 휴대폰 번호(형식: `010-0000-0000`) | 권장 |
| `temporaryToken` | String | 소셜 가입용 임시 토큰(OAuth) | N |

주의

- 현재 구현상 `phoneNumber`가 `null`이면 서버 내부 예외가 발생할 수 있어 **항상 값을 보내는 것을 권장**합니다.
- `temporaryToken`이 있는 소셜 가입에서도 DTO 유효성 검증 때문에 `loginId/password/email`이 비어있으면 `400`이 발생할 수 있어 **비어있지 않게 전달**하는 것을 권장합니다.

요청 예시 (로컬 가입)

```json
{
  "loginId": "user1234",
  "password": "pw1234!",
  "email": "user@example.com",
  "birthDate": "2000-01-01",
  "phoneNumber": "010-1234-5678"
}
```

요청 예시 (소셜 가입 완료)

```json
{
  "loginId": "oauth1234",
  "password": "oauth_dummy_password",
  "email": "user@example.com",
  "birthDate": "2000-01-01",
  "phoneNumber": "010-1234-5678",
  "temporaryToken": "<temporary_token>"
}
```

응답 (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `user` | Object | 가입된 사용자 정보 |
| `tokens` | Object | 발급된 토큰 |

`user`

| 필드명 | 타입 | 설명 |
|---|---|---|
| `userId` | String(UUID) | 사용자 식별자 |
| `email` | String | 이메일 |
| `birthDate` | String(`YYYY-MM-DD`) | 생년월일 |
| `phoneNumber` | String | 휴대폰 번호(없을 수 있음) |

`tokens`는 “로그인” 응답과 동일합니다.

응답 예시

```json
{
  "user": {
    "userId": "00000000-0000-0000-0000-000000000000",
    "email": "user@example.com",
    "birthDate": "2000-01-01",
    "phoneNumber": "010-1234-5678"
  },
  "tokens": {
    "accessToken": "string",
    "accessExpiresAt": "2026-01-01T00:00:00Z",
    "refreshToken": "string",
    "refreshExpiresAt": "2026-01-15T00:00:00Z"
  }
}
```

---

### 토큰 재발급 (Refresh)

`POST /auth/refresh`

- 설명: 리프레시 토큰으로 새 토큰을 발급합니다. (리프레시 토큰 로테이션)
- 인증: 불필요

요청 바디

| 필드명 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `refreshToken` | String | 리프레시 토큰 | Y |

요청 예시

```json
{ "refreshToken": "<refreshToken>" }
```

응답 (200 OK)

- 로그인 응답과 동일 스키마

---

### 로그아웃

`POST /auth/logout`

- 설명: 로그아웃합니다(서버에 저장된 리프레시 토큰 삭제).
- 인증: 필요

요청 바디: 없음

응답 (200 OK)

- 본문 없음

---

## 2. 사용자 (Members)

### 내 정보 조회

`GET /members/me`

- 설명: 로그인한 사용자의 정보를 조회합니다.
- 인증: 필요

응답 (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `userId` | String(UUID) | 사용자 식별자 |
| `email` | String | 이메일 |
| `birthDate` | String(`YYYY-MM-DD`) | 생년월일 |
| `phoneNumber` | String | 휴대폰 번호(없을 수 있음) |

응답 예시

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "email": "user@example.com",
  "birthDate": "2000-01-01",
  "phoneNumber": "010-1234-5678"
}
```

---

### 회원 정보 수정

`PATCH /members/{userId}`

- 설명: 특정 사용자의 정보를 수정합니다(부분 업데이트).
- 인증: 필요

경로 파라미터

| 이름 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `userId` | String(UUID) | 수정할 사용자 식별자 | Y |

요청 바디

| 필드명 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `loginId` | String | 변경할 로그인 ID (규칙: `^[a-zA-Z0-9]{4,20}$`) | N |
| `password` | String | 변경할 비밀번호 | N |
| `birthDate` | String(`YYYY-MM-DD`) | 변경할 생년월일 | N |
| `phoneNumber` | String | 변경할 휴대폰 번호 (`010-0000-0000`) | N |

요청 예시

```json
{
  "loginId": "user5678",
  "phoneNumber": "010-9999-8888"
}
```

응답 (200 OK)

- 본문은 “내 정보 조회” 응답과 동일

---

### 회원 탈퇴(Soft Delete)

`DELETE /members/{userId}`

- 설명: 사용자를 탈퇴 처리(soft delete)합니다.
- 인증: 현재 구현상 불필요
  - 보안 상 변경될 가능성이 있으므로, 프론트에서는 “인증 필요” 전제로 구현하는 것을 권장합니다.

경로 파라미터

| 이름 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `userId` | String(UUID) | 탈퇴할 사용자 식별자 | Y |

응답 (200 OK)

- 본문 없음

---

## 3. 관심 종목 (Favorite Stocks)

### 관심 종목 추가

`POST /members/{userId}/favorite-stocks/{stockId}`

- 설명: 사용자의 관심 종목을 추가합니다.
- 인증: 필요

경로 파라미터

| 이름 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `userId` | String(UUID) | 사용자 식별자 | Y |
| `stockId` | Number(Long) | 종목 식별자 | Y |

응답 (200 OK)

| 필드명 | 타입 | 설명 |
|---|---|---|
| `stockId` | Number | 종목 식별자 |
| `name` | String | 종목명 |
| `userId` | String(UUID) | 사용자 식별자 |

응답 예시

```json
{
  "stockId": 1,
  "name": "삼성전자",
  "userId": "00000000-0000-0000-0000-000000000000"
}
```

---

### 관심 종목 제거

`DELETE /members/{userId}/favorite-stocks/{stockId}`

- 설명: 사용자의 관심 종목을 제거합니다.
- 인증: 필요

경로 파라미터

| 이름 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `userId` | String(UUID) | 사용자 식별자 | Y |
| `stockId` | Number(Long) | 종목 식별자 | Y |

응답 (200 OK)

- 본문은 “관심 종목 추가” 응답과 동일

---

### 관심 종목 목록 조회

`GET /members/{userId}/favorite-stocks`

- 설명: 특정 사용자의 관심 종목 목록을 조회합니다.
- 인증: 불필요

경로 파라미터

| 이름 | 타입 | 설명 | 필수 여부 |
|---|---|---|:---:|
| `userId` | String(UUID) | 사용자 식별자 | Y |

응답 (200 OK)

- 배열 형태이며 요소 스키마는 “관심 종목 추가” 응답과 동일

응답 예시

```json
[
  { "stockId": 1, "name": "삼성전자", "userId": "00000000-0000-0000-0000-000000000000" }
]
```

