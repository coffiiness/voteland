package com.team.voteland.domain.vote.domain;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.support.error.CoreException;
import com.team.voteland.support.error.ErrorType;
import com.team.voteland.domain.vote.api.v1.response.VoteDetailResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteInfoResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteOptionResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteOptionResultResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteResultResponse;
import com.team.voteland.storage.db.core.vote.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VoteService {

    private final VoteRepository voteRepository;

    private final VoteOptionRepository voteOptionRepository;

    private final VoteRecordRepository voteRecordRepository;

    @Autowired
    public VoteService(VoteRepository voteRepository, VoteOptionRepository voteOptionRepository,
            VoteRecordRepository voteRecordRepository) {
        this.voteRepository = voteRepository;
        this.voteOptionRepository = voteOptionRepository;
        this.voteRecordRepository = voteRecordRepository;
    }

    /**
     * 투표 생성
     */
    public void createVote(Long userId, String title, String description, VoteType voteType, List<String> options,
            LocalDateTime deadline) {
        VoteEntity voteEntity = new VoteEntity(userId, title, description, voteType, deadline);
        voteRepository.save(voteEntity);

        List<VoteOptionEntity> voteOptions = new ArrayList<>();
        for (int sequence = 0; sequence < options.size(); sequence++) {
            VoteOptionEntity voteOptionEntity = new VoteOptionEntity(voteEntity.getId(), options.get(sequence),
                    sequence);
            voteOptions.add(voteOptionEntity);
        }
        voteOptionRepository.saveAll(voteOptions);
    }

    /**
     * 투표 조회
     */
    public List<VoteInfo> getVoteInfos() {
        List<Vote> votes = voteRepository.findAll().stream().map(Vote::from).toList();

        List<VoteInfo> voteInfos = new ArrayList<>();
        for (Vote vote : votes) {
            long optionCount = voteOptionRepository.countByVoteId(vote.id());
            long voterCount = voteRecordRepository.countDistinctUserIdByVoteId(vote.id());

            VoteInfo voteInfo = VoteInfo.of(vote, (int) optionCount, (int) voterCount);
            voteInfos.add(voteInfo);
        }

        return voteInfos;
    }

    /**
     * 투표 상세 조회
     */
    public VoteDetailResponse getVoteDetail(Long voteId) {
        VoteEntity voteEntity = voteRepository.findById(voteId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        List<VoteOptionEntity> voteOptionEntities = voteOptionRepository.findAllByVoteId(voteId);
        long participantCount = voteRecordRepository.countDistinctUserIdByVoteId(voteId);

        LocalDateTime now = LocalDateTime.now();
        VoteStatus voteStatus = now.isAfter(voteEntity.getDeadline()) ? VoteStatus.CLOSED : VoteStatus.OPEN;

        String remainingTime;
        if (voteStatus == VoteStatus.CLOSED) {
            remainingTime = "마감됨";
        } else {
            long days = Duration.between(now, voteEntity.getDeadline()).toDays();
            remainingTime = days + "일 남음";
        }

        List<VoteOptionResponse> options = voteOptionEntities.stream()
                .map(option -> new VoteOptionResponse(option.getId(), option.getContent()))
                .toList();

        return new VoteDetailResponse(
                voteEntity.getId(),
                voteStatus,
                voteEntity.getTitle(),
                voteEntity.getDescription(),
                voteEntity.getCreatedAt(),
                voteEntity.getDeadline(),
                remainingTime,
                voteEntity.getVoteType(),
                (int) participantCount,
                options);
    }

    /**
     * 투표제출
     */
    @Transactional
    public void submitVote(Long userId, Long voteId, List<Long> itemIds) {
        VoteEntity voteEntity = voteRepository.findById(voteId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(voteEntity.getDeadline())) {
            throw new CoreException(ErrorType.VALIDATION_ERROR, "투표가 마감되었습니다.");
        }

        // 사용자 중복 투표 체크
        long userVoteCount = voteRecordRepository.countByVoteId(voteEntity.getId());

        // 투표 타입 검증 (단일 투표인데 다중 선택을 한 경우)
        if (voteEntity.getVoteType() == VoteType.SINGLE && itemIds.size() > 1) {
            throw new CoreException(ErrorType.VALIDATION_ERROR, "단일 투표는 하나의 항목만 선택할 수 있습니다.");
        }

        // 옵션 검증 및 레코드 생성

        // 기존 투표 삭제 (재투표 지원)
        voteRecordRepository.deleteByVoteIdAndUserId(voteId, userId);

        List<VoteRecordEntity> records = new ArrayList<>();
        for (Long itemId : itemIds) {
            // 해당 투표의 항목인지 검증
            VoteOptionEntity option = voteOptionRepository.findById(itemId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 항목입니다."));

            if (!option.getVoteId().equals(voteId)) {
                throw new CoreException(ErrorType.VALIDATION_ERROR, "해당 투표의 항목이 아닙니다.");
            }

            records.add(new VoteRecordEntity(voteId, userId, itemId));
        }

        try {
            voteRecordRepository.saveAll(records);
        } catch (Exception e) {
            // 데이터 무결성 예외 처리 (이미 참여한 경우 등)
            throw new CoreException(ErrorType.VALIDATION_ERROR, "이미 참여한 투표이거나 중복 투표입니다.");
        }

    }

    /**
     * 투표 결과 조회
     */
    @Transactional(readOnly = true)
    public VoteResultResponse getVoteResult(Long voteId) {
        VoteEntity voteEntity = voteRepository.findById(voteId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        List<VoteOptionEntity> voteOptionEntities = voteOptionRepository.findAllByVoteId(voteId);
        long totalVoteCount = voteRecordRepository.countDistinctUserIdByVoteId(voteId);

        LocalDateTime now = LocalDateTime.now();
        VoteStatus voteStatus = now.isAfter(voteEntity.getDeadline()) ? VoteStatus.CLOSED : VoteStatus.OPEN;

        // 각 옵션별 득표수, 비율 계산
        List<VoteOptionResultResponse> optionResults = new ArrayList<>();
        for (VoteOptionEntity option : voteOptionEntities) {
            long count = voteRecordRepository.countByVoteOptionId(option.getId());
            double ratio = totalVoteCount == 0 ? 0.0 : (double) count / totalVoteCount * 100.0;
            optionResults.add(new VoteOptionResultResponse(option.getId(), option.getContent(), (int) count, ratio, 0));
        }

        // 득표수 기준 내림차순 정렬
        optionResults.sort(Comparator.comparingInt(VoteOptionResultResponse::voteCount).reversed());

        // 순위 매기기 (동점자 처리 고려)
        List<VoteOptionResultResponse> rankedOptions = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < optionResults.size(); i++) {
            VoteOptionResultResponse current = optionResults.get(i);
            // 이전 항목보다 득표수가 적으면 순위 증가, 같으면 동일 순위 유지
            if (i > 0 && current.voteCount() < optionResults.get(i - 1).voteCount()) {
                rank++;
            }
            rankedOptions.add(new VoteOptionResultResponse(current.id(), current.content(), current.voteCount(),
                    current.voteRatio(), rank));
        }

        return new VoteResultResponse(
                voteEntity.getId(),
                voteEntity.getTitle(),
                voteEntity.getDescription(),
                voteStatus,
                (int) totalVoteCount,
                voteEntity.getDeadline(),
                LocalDateTime.now(),
                rankedOptions);
    }
}
