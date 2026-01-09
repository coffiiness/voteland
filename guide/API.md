# API 가이드

---

## 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL (로컬) | `http://localhost:8080` |
| Base URL (프로덕션) | `https://api.voteland.com` |
| Content-Type | `application/json` |

---

## 응답 형식

모든 API는 동일한 응답 구조를 따릅니다.

### 성공 응답

```json
{
  "result": "SUCCESS",
  "data": {
    "id": 1,
    "email": "test@example.com",
    "name": "테스트유저"
  },
  "error": null
}
```

### 실패 응답

```json
{
  "result": "ERROR",
  "data": null,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다"
  }
}
```

---

## 인증

JWT Bearer 토큰을 사용합니다.

```bash
Authorization: Bearer {accessToken}
```

| 토큰 종류 | 유효기간 | 용도 |
|----------|---------|------|
| Access Token | 1시간 | API 인증 |
| Refresh Token | 7일 | Access Token 갱신 |

---

## User API

### 회원가입

새로운 사용자를 등록합니다.

```
POST /api/v1/users/signup
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | O | 이메일 (유효한 이메일 형식) |
| password | string | O | 비밀번호 |
| name | string | O | 이름 |

**요청 예시**

```bash
curl -X POST http://localhost:8080/api/v1/users/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "테스트유저"
  }'
```

**응답 예시 (201 Created)**

```json
{
  "id": 1,
  "email": "test@example.com",
  "name": "테스트유저",
  "role": "USER",
  "createdAt": "2024-01-15T10:30:00"
}
```

**에러 응답**

| 상황 | HTTP Status | Error Code |
|------|-------------|------------|
| 이메일 중복 | 400 | DUPLICATE_EMAIL |
| 유효하지 않은 이메일 형식 | 400 | INVALID_EMAIL |

---

### 로그인

이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.

```
POST /api/v1/users/login
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | O | 이메일 |
| password | string | O | 비밀번호 |

**요청 예시**

```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**응답 예시 (200 OK)**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "test@example.com",
    "name": "테스트유저",
    "role": "USER",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**에러 응답**

| 상황 | HTTP Status | Error Code |
|------|-------------|------------|
| 이메일/비밀번호 불일치 | 400 | INVALID_CREDENTIALS |

---

### 내 정보 조회

현재 로그인한 사용자의 정보를 조회합니다.

```
GET /api/v1/users/me
```

**Headers**

```
Authorization: Bearer {accessToken}
```

**요청 예시**

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**응답 예시 (200 OK)**

```json
{
  "id": 1,
  "email": "test@example.com",
  "name": "테스트유저",
  "role": "USER",
  "createdAt": "2024-01-15T10:30:00"
}
```

**에러 응답**

| 상황 | HTTP Status | Error Code |
|------|-------------|------------|
| 인증 토큰 없음 | 401 | UNAUTHORIZED |
| 토큰 만료 | 401 | TOKEN_EXPIRED |

---

### 회원 탈퇴

현재 로그인한 사용자의 계정을 삭제합니다.

```
DELETE /api/v1/users/me
```

**Headers**

```
Authorization: Bearer {accessToken}
```

**요청 예시**

```bash
curl -X DELETE http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**응답 예시 (204 No Content)**

응답 본문 없음

**에러 응답**

| 상황 | HTTP Status | Error Code |
|------|-------------|------------|
| 인증 토큰 없음 | 401 | UNAUTHORIZED |

---

## 공통 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|-------------|------|
| INVALID_INPUT | 400 | 잘못된 입력값 |
| UNAUTHORIZED | 401 | 인증 필요 |
| TOKEN_EXPIRED | 401 | 토큰 만료 |
| FORBIDDEN | 403 | 권한 없음 |
| NOT_FOUND | 404 | 리소스 없음 |
| INTERNAL_ERROR | 500 | 서버 내부 오류 |

---

## API 테스트 시나리오

### 1. 회원가입 → 로그인 → 내 정보 조회

```bash
# 1. 회원가입
curl -X POST http://localhost:8080/api/v1/users/signup \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123", "name": "테스트유저"}'

# 2. 로그인 (토큰 저장)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}' \
  | jq -r '.accessToken')

# 3. 내 정보 조회
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### 2. 회원 탈퇴 테스트

```bash
# 로그인 후 탈퇴
curl -X DELETE http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## Postman Collection

Postman을 사용하는 경우 다음 환경 변수를 설정합니다.

| 변수명 | 값 |
|--------|-----|
| `baseUrl` | `http://localhost:8080` |
| `accessToken` | 로그인 후 발급받은 토큰 |

**Pre-request Script (자동 토큰 설정)**

```javascript
// 로그인 응답에서 토큰 자동 저장
if (pm.response.json().accessToken) {
    pm.environment.set("accessToken", pm.response.json().accessToken);
}
```

---

## 모니터링 엔드포인트

| 엔드포인트 | 설명 |
|------------|------|
| `/health` | 헬스 체크 |
| `/actuator/prometheus` | Prometheus 메트릭 |
