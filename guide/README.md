# Voteland 프로젝트 가이드

> 신규 개발자를 위한 온보딩 문서

---

## 문서 목차

| 문서 | 설명 |
|------|------|
| [README.md](./README.md) | 빠른 시작 및 프로젝트 개요 (현재 문서) |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 아키텍처 다이어그램 및 모듈 구조 |
| [TDD.md](./TDD.md) | TDD 개발 가이드 및 User 도메인 예시 |
| [API.md](./API.md) | API 엔드포인트 및 사용법 |

---

## 빠른 시작 가이드

### 1. 환경 요구사항

- Java 17
- Gradle 8.x (Wrapper 포함)

### 2. 프로젝트 설정

```bash
# 프로젝트 클론
git clone <repository-url>
cd voteland

# Git Hooks 활성화 (커밋 시 자동 포맷 검사)
git config core.hookspath .githooks
```

### 3. 로컬 실행

```bash
# 빌드
./gradlew build

# 애플리케이션 실행 (H2 인메모리 DB)
./gradlew :core:core-api:bootRun --args='--spring.profiles.active=local'
```

### 4. 실행 확인

```bash
# 헬스 체크
curl http://localhost:8080/health
```

---

## 프로젝트 개요

| 항목 | 값 |
|------|-----|
| Spring Boot | 3.5.3 |
| Java | 17 |
| 빌드 도구 | Gradle (멀티 모듈) |
| 패키지 | com.team.voteland |

---

## 주요 명령어

### 빌드 & 실행

```bash
./gradlew build                              # 빌드
./gradlew :core:core-api:bootRun             # 실행
```

### 테스트

```bash
./gradlew test          # 전체 테스트
./gradlew unitTest      # 단위 테스트
./gradlew contextTest   # 통합 테스트
./gradlew restDocsTest  # REST Docs 생성
```

### 코드 스타일

```bash
./gradlew checkFormat   # 포맷 검사
./gradlew format        # 자동 포맷팅
```

---

## 런타임 프로파일

| 프로파일 | 용도 | 데이터베이스 |
|---------|------|-------------|
| local | 로컬 개발 (오프라인) | H2 인메모리 |
| prod | 프로덕션 환경 | MySQL |

---

## 다음 단계

1. [ARCHITECTURE.md](./ARCHITECTURE.md) - 프로젝트 구조 이해하기
2. [TDD.md](./TDD.md) - TDD 기반 개발 방법 익히기
3. [API.md](./API.md) - API 사용법 확인하기