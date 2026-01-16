package com.team.voteland.domain.vote.domain;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.domain.vote.api.v1.response.VoteDetailResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteItemResponse;
import com.team.voteland.domain.vote.api.v1.request.VoteSubmitRequest;
import com.team.voteland.domain.vote.api.v1.response.VoteSubmitResponse;
import com.team.voteland.storage.db.core.vote.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
            long voterCount = voteRecordRepository.countByVoteId(vote.id());

            VoteInfo voteInfo = VoteInfo.of(vote, (int) optionCount, (int) voterCount);
            voteInfos.add(voteInfo);
        }

        return voteInfos;
    }

    /**
     * 투표 상세 조회
     */
    // 투표 정보 조회 및 도메인 객체(Vote)로 변환
    public VoteDetailResponse getVoteDetail(Long voteId) {
        VoteEntity voteEntity = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 투표입니다."));

        // 현재 시간을 먼저 구함
        LocalDateTime now = LocalDateTime.now();

        // Vote 객체 생성 (VoteEntity의 상태는 사용 X)
        Vote vote = Vote.from(voteEntity);

        // 투표 옵션 조회
        List<VoteOptionEntity> voteOptions = voteOptionRepository.findAllByVoteId(vote.id());
        long voterCount = voteRecordRepository.countByVoteId(voteId);

        // 마감까지 남은 시간 계산
        String remainingTime = calculateRemainingTime(now, vote.deadline());

        // 현재 시점 기준으로 상태 계산 (일관성 유지)
        VoteStatus currentStatus = now.isBefore(vote.deadline()) ? VoteStatus.OPEN : VoteStatus.CLOSED;

        // VoteOptionEntity를 DTO로 변환
        List<VoteItemResponse> items = voteOptions.stream()
                .map(option -> new VoteItemResponse(option.getId(),
                        option.getContent()))
                .toList();

        // 최종 상세 조회 응답 객체 생성 및 반환
        return new VoteDetailResponse(
                vote.id(),
                currentStatus,
                vote.title(),
                vote.description(),
                vote.createdAt(),
                vote.deadline(),
                remainingTime,
                vote.voteType(),
                (int) voterCount,
                items);
    }

    /**
     * 투표 하기
     */
    @Transactional
    public VoteSubmitResponse submitVote(Long voteId, Long userId, VoteSubmitRequest request) {
        // 1. 투표 조회 및 유효성 검증
        VoteEntity voteEntity = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 투표입니다."));
        Vote vote = Vote.from(voteEntity);

        // 2. 마감 및 상태 확인
        if (vote.voteStatus() == VoteStatus.CLOSED || LocalDateTime.now().isAfter(vote.deadline())) {
            throw new IllegalArgumentException("종료된 투표입니다.");
        }

        // 3. 투표 타입 검증
        List<Long> itemIds = request.itemIds();
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("선택한 항목이 없습니다.");
        }
        if (vote.voteType() == VoteType.SINGLE && itemIds.size() > 1) {
            throw new IllegalArgumentException("단일 투표에서는 하나의 항목만 선택할 수 있습니다.");
        }

        // 4. 옵션 검증 (해당 투표에 속한 옵션인지)
        List<VoteOptionEntity> options = voteOptionRepository.findAllByVoteId(voteId);
        Map<Long, VoteOptionEntity> optionMap = options.stream()
                .collect(Collectors.toMap(VoteOptionEntity::getId, Function.identity()));

        for (Long itemId : itemIds) {
            if (!optionMap.containsKey(itemId)) {
                throw new IllegalArgumentException("유효하지 않은 항목 ID입니다: " + itemId);
            }
        }

        // 5. 기존 투표 확인 및 제거 (재투표 로직)
        List<VoteRecordEntity> existingRecords = voteRecordRepository.findAllByVoteIdAndUserId(voteId, userId);
        if (!existingRecords.isEmpty()) {
            for (VoteRecordEntity record : existingRecords) {
                VoteOptionEntity oldOption = optionMap.get(record.getVoteOptionId());   // 기존 옵션 득표수 감소
                if (oldOption != null) {
                    oldOption.decreaseVoteCount();
                }
            }
            voteRecordRepository.deleteAll(existingRecords);
            voteRecordRepository.flush();
        }

        // 6. 새 투표 저장
        List<VoteRecordEntity> newRecords = new ArrayList<>();
        List<Long> votedItemIds = new ArrayList<>();
        for (Long itemId : itemIds) {
            VoteOptionEntity option = optionMap.get(itemId);
            option.increaseVoteCount();     // 득표수 증가
            newRecords.add(new VoteRecordEntity(voteId, userId, itemId));
            votedItemIds.add(itemId);
        }

        voteRecordRepository.saveAll(newRecords);
        voteOptionRepository.flush();

        return new VoteSubmitResponse("투표 제출이 완료되었습니다.", votedItemIds);
    }

    // 마감 기한까지 남은 시간을 계산해 문자열로 반환
    private String calculateRemainingTime(LocalDateTime now, LocalDateTime deadline) {
        // 1. 이미 마감된 경우
        if (now.isAfter(deadline)) {
            return "투표 종료";
        }
        // 2. 시간 차이 계산
        java.time.Duration duration = java.time.Duration.between(now, deadline);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        // 3. formating: 1시간 이상 남았으면 '분'까지만, 1시간 미만이면 '초'까지 표시
        if (hours > 0) {
            return String.format("%d시간 %d분 남음", hours, minutes);
        } else {
            return String.format("%d분 %d초 남음", minutes, seconds);
        }
    }

}
