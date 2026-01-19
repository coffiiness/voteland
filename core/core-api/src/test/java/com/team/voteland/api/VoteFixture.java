package com.team.voteland.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.domain.vote.api.v1.request.CreateVoteRequest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class VoteFixture extends BaseFixture {

	public VoteFixture(TestRestTemplate restTemplate, ObjectMapper objectMapper) {
		super(restTemplate, objectMapper);
	}

	public String randomTitle() {
		return "title-" + UUID.randomUUID();
	}

	public String randomDescription() {
		return "description-" + UUID.randomUUID();
	}

	public VoteType randomVoteType() {
		int random = new Random().nextInt(VoteType.values().length);
		return VoteType.values()[random];
	}

	public List<String> randomOptions() {
		List<String> options = new ArrayList<>();
		int random = new Random().nextInt(5) + 2;
		for (int i = 0; i < random; i++) {
			options.add("option-" + i + "-" + UUID.randomUUID());
		}
		return options;
	}

	public LocalDateTime randomDeadline() {
		return LocalDateTime.now().plusHours(new Random().nextInt(6) + 1);
	}

	public ApiResponse<Void> createVote(String title, String description, VoteType voteType, List<String> options,
			LocalDateTime deadline) {
		CreateVoteRequest request = new CreateVoteRequest(title, description, voteType, options, deadline);
		return post("/api/v1/votes", request, Void.class);
	}
}
