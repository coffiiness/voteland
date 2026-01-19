package com.team.voteland.api;

import java.util.UUID;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.domain.user.api.v1.request.LoginRequest;
import com.team.voteland.domain.user.api.v1.request.SignUpRequest;
import com.team.voteland.domain.user.api.v1.response.LoginResponse;
import com.team.voteland.domain.user.api.v1.response.UserResponse;

@Component
public class UserFixture extends BaseFixture {

	public UserFixture(TestRestTemplate restTemplate, ObjectMapper objectMapper) {
		super(restTemplate, objectMapper);
	}

	// ==================== Random Data Generators ====================

	public String randomEmail() {
		return "user-" + UUID.randomUUID() + "@test.com";
	}

	public String randomPassword() {
		return "password123";
	}

	public String randomName() {
		return "TestUser-" + UUID.randomUUID().toString().substring(0, 8);
	}

	// ==================== API Calls ====================

	public ApiResponse<UserResponse> signUp() {
		return signUp(randomEmail(), randomPassword(), randomName());
	}

	public ApiResponse<UserResponse> signUp(String email, String password, String name) {
		SignUpRequest request = new SignUpRequest(email, password, name);
		return post("/api/v1/users/signup", request, UserResponse.class);
	}

	public ApiResponse<LoginResponse> login(String email, String password) {
		LoginRequest request = new LoginRequest(email, password);
		return post("/api/v1/users/login", request, LoginResponse.class);
	}

	public ApiResponse<UserResponse> me(String token) {
		return get("/api/v1/users/me", token, UserResponse.class);
	}

	public ApiResponse<Void> deleteMe(String token) {
		return delete("/api/v1/users/me", token, Void.class);
	}

	// ==================== Convenience Methods ====================

	/**
	 * 새 사용자를 생성하고 로그인하여 액세스 토큰을 반환합니다.
	 */
	public String createUserAndGetToken() {
		String email = randomEmail();
		String password = randomPassword();
		signUp(email, password, randomName());
		ApiResponse<LoginResponse> loginResponse = login(email, password);
		return loginResponse.getData().accessToken();
	}

	/**
	 * 지정된 정보로 사용자를 생성하고 로그인하여 액세스 토큰을 반환합니다.
	 */
	public String createUserAndGetToken(String email, String password, String name) {
		signUp(email, password, name);
		ApiResponse<LoginResponse> loginResponse = login(email, password);
		return loginResponse.getData().accessToken();
	}

}
