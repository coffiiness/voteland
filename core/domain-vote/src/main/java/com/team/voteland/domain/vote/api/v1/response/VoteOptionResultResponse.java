package com.team.voteland.domain.vote.api.v1.response;

public record VoteOptionResultResponse(Long id, String content, int voteCount, double voteRatio, int rank) {
}
