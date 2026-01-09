# 아키텍처 가이드

---

## 모듈화된 모놀리식 (Modular Monolith)

이 프로젝트는 **모듈화된 모놀리식** 아키텍처를 채택합니다.

### 왜 모듈화된 모놀리식인가?

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│   전통 모놀리식              모듈화된 모놀리식           마이크로서비스         │
│                                                                             │
│   ┌───────────┐            ┌───────────────┐         ┌─────┐  ┌─────┐      │
│   │           │            │ ┌───┐ ┌───┐   │         │     │  │     │      │
│   │  혼재된   │            │ │ A │ │ B │   │         │  A  │  │  B  │      │
│   │   코드    │     →      │ └───┘ └───┘   │    →    │     │  │     │      │
│   │           │            │ ┌───┐ ┌───┐   │         └──┬──┘  └──┬──┘      │
│   │           │            │ │ C │ │ D │   │            │        │         │
│   └───────────┘            │ └───┘ └───┘   │         ┌──┴──┐  ┌──┴──┐      │
│                            │  명확한 경계   │         │  C  │  │  D  │      │
│   - 코드 뒤섞임            │               │         └─────┘  └─────┘      │
│   - 수정 시 영향 범위 불명  │ - 단일 배포    │         - 분산 트랜잭션       │
│                            │ - 명확한 경계  │         - 네트워크 통신        │
│                            │ - MSA 전환 용이│         - 배포 복잡도 증가     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

| 특성 | 전통 모놀리식 | 모듈화된 모놀리식 | 마이크로서비스 |
|------|-------------|-----------------|---------------|
| 배포 | 단일 | 단일 | 개별 |
| 모듈 경계 | 없음 | 명확함 | 명확함 |
| 트랜잭션 | 용이 | 용이 | 분산 트랜잭션 필요 |
| 운영 복잡도 | 낮음 | 낮음 | 높음 |
| MSA 전환 | 어려움 | 용이 | - |

**모듈화된 모놀리식은 마이크로서비스의 설계 원칙을 지키면서 단일 배포의 장점을 유지합니다.**

---

## 모듈 간 경계 설정

모듈 간의 명확한 경계가 모듈화된 모놀리식의 핵심입니다.

```
┌────────────────────────────────────────────────────────────────────┐
│                         모듈화된 모놀리식                            │
│                         (도메인별 모듈 분리)                         │
│                                                                    │
│  ┌──────────────┐       ┌──────────────┐       ┌──────────────┐   │
│  │  User Module │       │  Vote Module │       │ Other Module │   │
│  │   명확한 경계  │       │   명확한 경계  │       │   명확한 경계  │   │
│  └──────────────┘       └──────────────┘       └──────────────┘   │
│         │                      │                      │           │
│         │    Public API 호출    │      Event 발행      │           │
│         │◄────────────────────►│◄────────────────────►│           │
│         │       (동기식)        │       (비동기)        │           │
│                                                                    │
│                          ┌──────────┐                              │
│                          │  공유 DB  │                              │
│                          └──────────┘                              │
└────────────────────────────────────────────────────────────────────┘
```

### 1. Public API로 호출 (동기식)

다른 모듈의 정보가 필요할 때는 **Public API(Reader 인터페이스)** 를 통해서만 접근합니다.

```java
// domain-vote에서 사용자 정보가 필요한 경우

// (O) 올바른 방법: Public API(Reader) 사용
@Service
public class VoteService {
    private final UserReader userReader;  // domain-user가 노출한 Public API

    public void createVote(Long userId) {
        User user = userReader.getUser(userId);  // Public API 호출
        // ...
    }
}

// (X) 금지: 직접 Repository 접근
@Service
public class VoteService {
    private final UserRepository userRepository;  // 다른 모듈 내부 접근 금지!

    public void createVote(Long userId) {
        UserEntity user = userRepository.findById(userId);  // 경계 침범!
    }
}

// (X) 금지: 직접 Service 접근
@Service
public class VoteService {
    private final UserService userService;  // 다른 모듈 내부 Service 접근 금지!
}
```

### 2. Event Bus로 호출 (비동기)

비동기 처리가 필요한 경우 **Event**를 통해 통신합니다.

```java
// domain-user: 이벤트 발행
@Service
public class UserService {
    private final DomainEventPublisher eventPublisher;

    public void deleteUser(Long userId) {
        // 사용자 삭제 로직...

        // 이벤트 발행 (다른 모듈에게 알림)
        eventPublisher.publish(UserDeletedEvent.of(userId, email));
    }
}

// domain-vote: 이벤트 구독
@Component
public class UserDeletedEventHandler {

    @EventListener
    @Async
    public void handle(UserDeletedEvent event) {
        // 해당 사용자의 투표 데이터 정리
    }
}
```

---

## 경계 침범 금지 규칙

| 구분 | 허용 | 금지 |
|------|------|------|
| 다른 모듈 데이터 조회 | Reader 인터페이스 사용 | Repository 직접 접근 |
| 다른 모듈 기능 호출 | Public API 사용 | 내부 Service 직접 호출 |
| 다른 모듈에 알림 | Event 발행 | 직접 메서드 호출 |
| DB 테이블 접근 | 자기 모듈 테이블만 | 다른 모듈 테이블 직접 조회 |

**코드 리뷰 시 체크포인트:**
- 다른 모듈의 Repository를 직접 주입받고 있지 않은가?
- 다른 모듈의 내부 Service를 직접 호출하고 있지 않은가?
- 모듈 경계를 침범하는 의존성이 있지 않은가?

---

## 협력 구조

```
                              모듈화된 모놀리식
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                           User 모듈                                  │   │
│   │  ┌──────────┐      ┌─────────────┐      ┌────────────────┐          │   │
│   │  │ User API │ ───► │ User Domain │ ───► │ User Repository│ ──┐      │   │
│   │  └──────────┘      └─────────────┘      └────────────────┘   │      │   │
│   └──────────────────────────────────────────────────────────────│──────┘   │
│                                    │                             │          │
│                              Event 발행                          │          │
│                                    │                             │          │
│   ┌────────────────────────────────│─────────────────────────────│──────┐   │
│   │                           Vote 모듈                          │      │   │
│   │  ┌──────────┐      ┌─────────────┐      ┌────────────────┐   │      │   │
│   │  │ Vote API │ ───► │ Vote Domain │ ───► │ Vote Repository│ ──┤      │   │
│   │  └──────────┘      └──────┬──────┘      └────────────────┘   │      │   │
│   │                           │                                  │      │   │
│   │                     Event Handler                            │      │   │
│   │                    (UserDeleted 구독)                         │      │   │
│   └──────────────────────────────────────────────────────────────│──────┘   │
│                                                                  │          │
│                                                           ┌──────▼──────┐   │
│                                                           │   공유 DB    │   │
│                                                           └─────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

각 모듈은 `API → Domain → Repository` 구조를 가지며, 모듈 간에는 **Public API** 또는 **Event**로만 통신합니다.

---

## 모듈 구조

```
voteland/
├── core/
│   ├── core-api/           # 실행 모듈 (모든 모듈을 조립)
│   ├── core-enum/          # 공유 Enum
│   ├── domain-user/        # User 모듈 (명확한 경계)
│   └── domain-vote/        # Vote 모듈 (명확한 경계)
├── storage/
│   └── db-core/            # 공유 DB 계층
├── support/
│   ├── security/           # 인증/인가
│   ├── event/              # Event Bus
│   ├── logging/            # 로깅
│   └── monitoring/         # 모니터링
├── clients/
│   └── client-example/     # 외부 API 클라이언트
└── tests/
    └── api-docs/           # API 문서화
```

---

## 도메인 모듈 내부 구조

각 도메인 모듈은 동일한 패키지 구조를 따릅니다.

```
domain-xxx/
└── src/main/java/com/team/voteland/domain/xxx/
    ├── api/                      # Public API (외부 노출)
    │   ├── XxxController.java    # REST API
    │   └── v1/
    │       ├── request/          # 요청 DTO
    │       └── response/         # 응답 DTO
    ├── domain/                   # 비즈니스 로직 (내부)
    │   ├── Xxx.java              # 도메인 모델 (Record)
    │   ├── XxxService.java       # 비즈니스 서비스
    │   ├── XxxReader.java        # 조회 인터페이스 (Public API)
    │   └── event/                # 도메인 이벤트
    └── infra/                    # 인프라 구현체 (내부)
        └── XxxReaderImpl.java
```

**외부 노출 (Public API):**
- `api/` - REST Controller
- `domain/XxxReader.java` - 다른 모듈이 사용하는 조회 인터페이스

**내부 구현 (접근 금지):**
- `domain/XxxService.java` - 비즈니스 로직
- `infra/` - 인프라 구현체

---

## 핵심 원칙

### 1. 도메인 모델과 JPA Entity 분리

```java
// 도메인 모델 (Record, 불변)
public record User(Long id, String email, String name, String role, LocalDateTime createdAt) {}

// JPA Entity (변경 가능, storage 모듈)
@Entity
public class UserEntity extends BaseEntity { ... }
```

### 2. Reader 인터페이스를 통한 모듈 간 통신

```java
// domain-user/domain/UserReader.java (Public API)
public interface UserReader {
    User getUser(Long userId);
    boolean existsByEmail(String email);
}

// domain-user/infra/UserReaderImpl.java (내부 구현)
@Component
public class UserReaderImpl implements UserReader {
    private final UserRepository userRepository;
    // ...
}
```

### 3. Event를 통한 비동기 통신

```java
// 이벤트 정의
public record UserDeletedEvent(Long userId, String email, LocalDateTime occurredAt)
    implements DomainEvent {

    public static UserDeletedEvent of(Long userId, String email) {
        return new UserDeletedEvent(userId, email, LocalDateTime.now());
    }
}
```

---

## 의존성 흐름

```
core-api (실행 모듈)
├── domain-user
├── domain-vote
├── core-enum
├── support:security
├── support:event
├── support:logging
├── support:monitoring
├── storage:db-core
└── clients:client-example

domain-user
├── core-enum
├── storage:db-core
├── support:security
└── support:event

domain-vote
├── storage:db-core
├── support:event
└── (domain-user의 Reader만 사용)
```

---

## 주요 파일 위치

| 용도 | 경로 |
|------|------|
| 애플리케이션 진입점 | `core/core-api/.../CoreApiApplication.java` |
| 전역 예외 처리 | `core/core-api/.../ApiControllerAdvice.java` |
| User Public API | `core/domain-user/.../UserReader.java` |
| User 비즈니스 로직 | `core/domain-user/.../UserService.java` |
| 이벤트 발행자 | `support/event/.../DomainEventPublisher.java` |
| 이벤트 핸들러 예시 | `core/domain-vote/.../UserDeletedEventHandler.java` |
