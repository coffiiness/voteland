# Contributing Guide

Voteland 프로젝트에 기여하기 위한 가이드입니다.

---

## 목차

1. [작업 흐름](#작업-흐름)
2. [브랜치 전략](#브랜치-전략)
3. [이슈 작성 가이드](#이슈-작성-가이드)
4. [커밋 가이드](#커밋-가이드)
5. [PR 가이드](#pr-가이드)

---

## 작업 흐름

```
1. 이슈 생성 → 2. 브랜치 생성 → 3. 작업 & 커밋 → 4. PR 생성 → 5. 코드 리뷰 → 6. Merge
```

### 상세 순서

| 단계 | 작업 | 설명 |
|:---:|------|------|
| 1 | 이슈 생성 | `[VL-XX] 작업 제목` 형식으로 생성 |
| 2 | 브랜치 생성 | `feature/VL-XX-작업단위` 형식으로 생성 |
| 3 | 작업 & 커밋 | TODO 단위로 커밋 |
| 4 | PR 생성 | develop 브랜치로 PR |
| 5 | 코드 리뷰 | 최소 1명 이상 리뷰 필수 |
| 6 | Merge | 리뷰어가 승인 후 Merge |

> **주의**: 본인이 직접 Merge하지 않습니다. 반드시 리뷰를 받은 후 진행합니다.

---

## 브랜치 전략

```
main ─────────────────────────────────────→ 배포용 (직접 푸시 금지)
  │
  └─ develop ─────────────────────────────→ 개발 통합 브랜치
       │
       ├─ feature/VL-01-user-signup ──────→ 기능 개발
       ├─ feature/VL-02-vote-create ──────→ 기능 개발
       └─ feature/VL-03-vote-result ──────→ 기능 개발
```

### 브랜치 네이밍 규칙

```
feature/VL-{이슈번호}-{작업내용}
```

**예시**
- `feature/VL-01-user-signup`
- `feature/VL-05-vote-crud`
- `feature/VL-10-vote-statistics`

---

## 이슈 작성 가이드

### 형식

```
Title: [VL-XX] 작업 제목
```

### 예시

```markdown
Title: [VL-03] 투표 생성 기능 구현

## 설명
사용자가 새로운 투표를 생성할 수 있는 기능 구현

## TODO
- [ ] Vote 도메인 모델 생성
- [ ] 투표 생성 API 구현 (POST /api/v1/votes)
- [ ] 투표 옵션 저장 로직 구현
- [ ] 투표 기간 설정 기능 구현
- [ ] 통합 테스트 작성
```

### 체크리스트

- [ ] 담당자(Assignees) 지정
- [ ] 라벨(Labels) 설정
- [ ] TODO는 커밋 단위로 세분화

---

## 커밋 가이드

### 형식

```
<type>(<scope>): <subject>
```

### Type 종류

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | feat(user): 회원가입 API 구현 |
| `fix` | 버그 수정 | fix(vote): 투표 마감 시간 오류 수정 |
| `refactor` | 코드 리팩토링 | refactor(user): 서비스 레이어 분리 |
| `docs` | 문서 수정 | docs(api): Swagger 설명 추가 |
| `test` | 테스트 코드 | test(vote): 투표 API 테스트 작성 |
| `build` | 빌드/의존성 설정 | build(gradle): JWT 의존성 추가 |
| `ci` | CI/CD 설정 | ci(actions): PR 자동 테스트 설정 |
| `chore` | 기타 변경사항 | chore: .gitignore 업데이트 |
| `style` | 코드 포맷팅 | style: 불필요한 import 제거 |
| `perf` | 성능 개선 | perf(vote): 투표 결과 캐싱 적용 |

### Scope (도메인)

| Scope | 설명 |
|-------|------|
| `common` | 공통 설정, 예외 처리, core-api |
| `user` | 사용자, 인증/인가 (domain-user) |
| `vote` | 투표 (domain-vote) |
| `security` | JWT, 인증 필터 (support:security) |
| `event` | 도메인 이벤트 (support:event) |
| `db` | 데이터베이스, 엔티티 (storage:db-core) |

### 커밋 예시

```bash
# 기능 추가
git commit -m "feat(user): 회원가입 API 구현"
git commit -m "feat(vote): 투표 생성 API 구현"

# 버그 수정
git commit -m "fix(vote): 마감된 투표에 투표 가능한 버그 수정"

# 테스트
git commit -m "test(user): 로그인 API 테스트 작성"

# 문서
git commit -m "docs(api): 투표 API 명세 추가"

# 빌드 설정
git commit -m "build(gradle): Spring Security 의존성 추가"
```

---

## PR 가이드

### 형식

```
Title: [VL-XX] 작업 제목
```

### PR 생성 전 체크리스트

- [ ] 로컬에서 `./gradlew test` 통과 확인
- [ ] 로컬에서 `./gradlew checkFormat` 통과 확인
- [ ] 불필요한 코드/주석 제거
- [ ] 커밋 메시지 컨벤션 준수
- [ ] 관련 이슈 연결

### PR 템플릿

PR 생성 시 자동으로 템플릿이 적용됩니다. (`.github/PULL_REQUEST_TEMPLATE.md`)

### 코드 리뷰 체크포인트

| 항목 | 확인 사항 |
|------|----------|
| 모듈 경계 | 다른 모듈의 Repository/Service 직접 접근 여부 |
| Public API | Reader 인터페이스를 통한 모듈 간 통신 여부 |
| 이벤트 | 비동기 처리가 필요한 곳에 Event 사용 여부 |
| 테스트 | TestRestTemplate 기반 API 테스트 작성 여부 |

### 주의사항

| 항목 | 규칙 |
|------|------|
| Merge 권한 | 본인 PR은 본인이 Merge 금지 |
| 리뷰 | 최소 1명 이상 Approve 필수 |
| 테스트 | `./gradlew test` 통과하는 코드만 PR |
| 이슈 연결 | PR에 관련 이슈 반드시 연결 |

### Merge 후 작업

1. 로컬 브랜치 삭제
```bash
git branch -d feature/VL-XX-xxx
```

2. 원격 브랜치 삭제 (GitHub에서 자동 삭제 설정 권장)

3. 이슈 Close

---

## 빠른 참조

### 새 작업 시작할 때

```bash
# 1. develop 브랜치 최신화
git checkout develop
git pull origin develop

# 2. 브랜치 생성
git checkout -b feature/VL-XX-작업내용

# 3. 작업 후 커밋
git add .
git commit -m "feat(scope): 작업 내용"

# 4. Push
git push origin feature/VL-XX-작업내용

# 5. GitHub에서 PR 생성
```

### 커밋 메시지 빠른 참조

```
feat     → 기능 추가
fix      → 버그 수정
refactor → 리팩토링
docs     → 문서
test     → 테스트
build    → 빌드/의존성
chore    → 기타
```
