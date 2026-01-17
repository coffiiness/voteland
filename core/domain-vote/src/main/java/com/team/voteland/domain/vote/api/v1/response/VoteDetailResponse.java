package com.team.voteland.domain.vote.api.v1.response;

import java.time.LocalDateTime;
import java.util.List;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;

public record VoteDetailResponse(Long id, VoteStatus voteStatus, String title, String description,
        LocalDateTime createdAt, LocalDateTime deadline, String remainingTime, VoteType voteType,
        Integer participantCount, List<VoteOptionResponse> items) {
}
