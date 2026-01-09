# TDD 개발 가이드

이 프로젝트는 **TDD(Test-Driven Development)** 기반으로 개발합니다.

---

## TDD 사이클

```
    ┌─────────┐      ┌─────────┐      ┌─────────┐
    │  RED    │ ───► │  GREEN  │ ───► │ REFACTOR│ ──┐
    │         │      │         │      │         │   │
    │ 실패하는 │      │ 통과하는 │      │ 코드    │   │
    │ 테스트   │      │ 최소코드 │      │ 개선    │   │
    │ 작성    │      │ 작성    │      │         │   │
    └─────────┘      └─────────┘      └─────────┘   │
         ▲                                          │
         └──────────────────────────────────────────┘
```

1. **RED**: 실패하는 테스트를 먼저 작성
2. **GREEN**: 테스트를 통과하는 최소한의 코드 작성
3. **REFACTOR**: 코드 품질 개선 (테스트는 계속 통과)

---

## 테스트 방식

| 테스트 종류 | 도구 | Mock 사용 | 용도 |
|------------|------|----------|------|
| API 테스트 | `TestRestTemplate` | X | 실제 동작 검증 |
| REST Docs | `MockMvc` | O | API 문서 생성 |

---

## 테스트 파일 구조

```
core/core-api/src/test/java/com/team/voteland/
├── api/
│   ├── VotelandApiTest.java      # 테스트 베이스 어노테이션
│   ├── TestFixture.java          # 테스트 헬퍼 클래스
│   └── users/
│       ├── signup/
│       │   └── POST_specs.java   # POST /api/v1/users/signup 테스트
│       ├── login/
│       │   └── POST_specs.java   # POST /api/v1/users/login 테스트
│       └── me/
│           └── GET_specs.java    # GET /api/v1/users/me 테스트
└── docs/
    ├── RestDocsTest.java         # REST Docs 베이스 클래스
    ├── RestDocsUtils.java        # REST Docs 유틸리티
    └── users/
        └── UserApiDocs.java      # User API 문서화 테스트
```

**네이밍 규칙:**
- 패키지: `api/{도메인}/{엔드포인트}/`
- 클래스: `{HTTP_METHOD}_specs.java`

---

## 테스트 베이스 클래스

### @VotelandApiTest

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public @interface VotelandApiTest {
}
```

### TestFixture

테스트 데이터 생성 및 인증을 위한 헬퍼 클래스입니다.

```java
@Component
public class TestFixture {

    private final TestRestTemplate restTemplate;
    private String accessToken;

    // 랜덤 데이터 생성
    public String randomEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    public String randomPassword() {
        return "password123";
    }

    public String randomName() {
        return "TestUser-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // 회원가입/로그인 헬퍼
    public UserResponse signUp(String email, String password, String name) { ... }
    public LoginResponse login(String email, String password) { ... }

    // 인증된 사용자 설정
    public void createUserThenSetAsDefault() { ... }

    // 인증 헤더
    public HttpEntity<Void> withAuth() { ... }
    public <T> HttpEntity<T> withAuth(T body) { ... }

    // REST 클라이언트
    public TestRestTemplate client() { ... }
}
```

---

## API 테스트 작성법

### 기본 구조

```java
@VotelandApiTest
@DisplayName("POST /api/v1/users/signup")
public class POST_specs {

    @Test
    void 올바르게_요청하면_201_Created_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(
            fixture.randomEmail(),
            fixture.randomPassword(),
            fixture.randomName()
        );

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .postForEntity("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }
}
```

**작성 규칙:**
- `@DisplayName`에 API 경로 명시
- 테스트 메서드명은 **한글**로 작성
- `// Arrange`, `// Act`, `// Assert` 주석 사용
- `TestFixture`는 파라미터로 `@Autowired` 주입

---

## User 도메인 TDD 예시

### Step 1: RED - 실패하는 테스트 작성

```java
// core/core-api/src/test/java/.../api/users/signup/POST_specs.java

@VotelandApiTest
@DisplayName("POST /api/v1/users/signup")
public class POST_specs {

    @Test
    void 올바르게_요청하면_201_Created_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(
            fixture.randomEmail(),
            fixture.randomPassword(),
            fixture.randomName()
        );

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .postForEntity("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void 이메일_형식이_올바르지_않으면_400_Bad_Request_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(
            "invalid-email",
            fixture.randomPassword(),
            fixture.randomName()
        );

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .postForEntity("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void 회원가입_성공시_사용자_정보를_올바르게_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String name = fixture.randomName();
        SignUpRequest request = new SignUpRequest(email, fixture.randomPassword(), name);

        // Act
        UserResponse response = fixture.client()
            .postForObject("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.name()).isEqualTo(name);
    }

    @Test
    void 회원가입_성공시_사용자_ID를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        SignUpRequest request = new SignUpRequest(
            fixture.randomEmail(),
            fixture.randomPassword(),
            fixture.randomName()
        );

        // Act
        UserResponse response = fixture.client()
            .postForObject("/api/v1/users/signup", request, UserResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
    }
}
```

### Step 2: GREEN - 테스트 통과하는 코드 작성

Request DTO, Response DTO, Domain Model, Service, Controller를 작성합니다.

### Step 3: REFACTOR - 코드 개선

테스트가 통과하는 상태에서 코드를 개선합니다.

---

## 로그인 API 테스트 예시

```java
// core/core-api/src/test/java/.../api/users/login/POST_specs.java

@VotelandApiTest
@DisplayName("POST /api/v1/users/login")
public class POST_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        fixture.signUp(email, password, fixture.randomName());

        LoginRequest request = new LoginRequest(email, password);

        // Act
        ResponseEntity<LoginResponse> response = fixture.client()
            .postForEntity("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent@test.com", fixture.randomPassword());

        // Act
        ResponseEntity<LoginResponse> response = fixture.client()
            .postForEntity("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        fixture.signUp(email, fixture.randomPassword(), fixture.randomName());

        LoginRequest request = new LoginRequest(email, "wrong-password");

        // Act
        ResponseEntity<LoginResponse> response = fixture.client()
            .postForEntity("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void 로그인_성공시_액세스_토큰을_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        String email = fixture.randomEmail();
        String password = fixture.randomPassword();
        fixture.signUp(email, password, fixture.randomName());

        LoginRequest request = new LoginRequest(email, password);

        // Act
        LoginResponse response = fixture.client()
            .postForObject("/api/v1/users/login", request, LoginResponse.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
    }
}
```

---

## 인증이 필요한 API 테스트 예시

```java
// core/core-api/src/test/java/.../api/users/me/GET_specs.java

@VotelandApiTest
@DisplayName("GET /api/v1/users/me")
public class GET_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        fixture.createUserThenSetAsDefault();

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .exchange("/api/v1/users/me", HttpMethod.GET, fixture.withAuth(), UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void 인증_토큰_없이_요청하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        fixture.clearAuth();

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .getForEntity("/api/v1/users/me", UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void 잘못된_토큰으로_요청하면_401_Unauthorized_상태코드를_반환한다(@Autowired TestFixture fixture) {
        // Arrange
        fixture.clearAuth();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");

        // Act
        ResponseEntity<UserResponse> response = fixture.client()
            .exchange("/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
```

---

## REST Docs 테스트 (Mock 사용)

API 문서 생성 시에만 Mock을 사용합니다.

```java
// core/core-api/src/test/java/.../docs/users/UserApiDocs.java

public class UserApiDocs extends RestDocsTest {

    private final UserService userService = mock(UserService.class);
    private final UserController userController = new UserController(userService);

    @BeforeEach
    @Override
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        super.setUp(restDocumentation);
        setUpMockMvc(userController, restDocumentation);
    }

    @Test
    void 회원가입_API_문서화() throws Exception {
        // given
        User user = new User(1L, "test@example.com", "홍길동", "USER", LocalDateTime.now());
        when(userService.signUp(anyString(), anyString(), anyString())).thenReturn(user);

        SignUpRequest request = new SignUpRequest("test@example.com", "password123", "홍길동");

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isCreated())
            .andDo(document("users/signup",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("email").description("사용자 이메일"),
                    fieldWithPath("password").description("비밀번호 (4-20자)"),
                    fieldWithPath("name").description("사용자 이름 (2-50자)")
                ),
                responseFields(
                    fieldWithPath("id").description("사용자 ID"),
                    fieldWithPath("email").description("사용자 이메일"),
                    fieldWithPath("name").description("사용자 이름"),
                    fieldWithPath("role").description("사용자 역할"),
                    fieldWithPath("createdAt").description("가입 일시")
                )
            ));
    }

    @Test
    void 로그인_API_문서화() throws Exception {
        // given
        User user = new User(1L, "test@example.com", "홍길동", "USER", LocalDateTime.now());
        UserService.LoginResult loginResult = new UserService.LoginResult(
            "access-token-example",
            "refresh-token-example",
            user
        );
        when(userService.login(anyString(), anyString())).thenReturn(loginResult);

        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // when & then
        mockMvc.perform(post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk())
            .andDo(document("users/login",
                responsePreprocessor(),
                requestFields(
                    fieldWithPath("email").description("사용자 이메일"),
                    fieldWithPath("password").description("비밀번호")
                ),
                responseFields(
                    fieldWithPath("accessToken").description("액세스 토큰"),
                    fieldWithPath("refreshToken").description("리프레시 토큰"),
                    fieldWithPath("user.id").description("사용자 ID"),
                    fieldWithPath("user.email").description("사용자 이메일"),
                    fieldWithPath("user.name").description("사용자 이름"),
                    fieldWithPath("user.role").description("사용자 역할"),
                    fieldWithPath("user.createdAt").description("가입 일시")
                )
            ));
    }
}
```

---

## 테스트 실행 명령어

```bash
# 전체 테스트 (API 테스트)
./gradlew test

# REST Docs 테스트 (문서 생성)
./gradlew restDocsTest
```

---

## TDD 체크리스트

새로운 API를 개발할 때 다음 순서로 진행합니다.

**1. 테스트 파일 생성**
- [ ] `api/{도메인}/{엔드포인트}/{HTTP_METHOD}_specs.java` 생성
- [ ] `@VotelandApiTest`, `@DisplayName` 추가

**2. 실패하는 테스트 작성**
- [ ] 정상 케이스: `올바르게_요청하면_{상태코드}_상태코드를_반환한다`
- [ ] 예외 케이스: `{조건}이면_{상태코드}_상태코드를_반환한다`
- [ ] 응답 검증: `{동작}_성공시_{결과}를_반환한다`

**3. 테스트 통과하는 코드 작성**
- [ ] Request/Response DTO
- [ ] Domain Model
- [ ] Service
- [ ] Controller

**4. 리팩토링**
- [ ] 코드 품질 개선
- [ ] 테스트 여전히 통과 확인

**5. REST Docs 테스트 작성**
- [ ] `docs/{도메인}/{도메인}ApiDocs.java`에 추가
- [ ] 요청/응답 필드 문서화