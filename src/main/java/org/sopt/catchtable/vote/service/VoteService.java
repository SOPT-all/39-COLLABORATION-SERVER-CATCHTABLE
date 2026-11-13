package org.sopt.catchtable.vote.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.service.StoreService;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.service.UserService;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.sopt.catchtable.vote.dto.request.SubmitBallotRequest;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.dto.response.MyActiveVoteResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final UserService userService;
    private final StoreService storeService;
    private final VoteCommandService voteCommandService;
    private final VoteDetailQueryService voteDetailQueryService;

    @Transactional
    public CreateVoteResponse createVote(Long userId, CreateVoteRequest request) {
        List<Long> storeIds = request.storeIds();
        Vote.validateCandidateStoreIds(storeIds);

        User hostUser = userService.getAuthorizedUser(userId);
        Map<Long, Store> storesById = storeService.getStoresByIds(storeIds);

        List<Store> stores = storeIds.stream()
                .map(storesById::get)
                .toList();

        Vote vote = voteRepository.save(Vote.create(hostUser));
        List<VoteCandidate> candidates = vote.createCandidates(stores);
        voteCandidateRepository.saveAll(candidates);

        return CreateVoteResponse.of(vote, candidates);
    }

    @Transactional(readOnly = true)
    public MyActiveVoteResponse getMyActiveVote(Long userId) {
        userService.getAuthorizedUser(userId);
        Optional<Vote> voteOptional =
                voteRepository.findFirstByHostUser_IdAndStatusOrderByCreatedAtDescIdDesc(
                        userId,
                        VoteStatus.OPEN
                );

        if (voteOptional.isEmpty()) {
            return MyActiveVoteResponse.builder()
                    .vote(null)
                    .build();
        }

        Vote vote = voteOptional.get();
        OffsetDateTime createdAt = vote.getCreatedAt()
                .atZone(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime();

        MyActiveVoteResponse.VoteResponse voteResponse =
                MyActiveVoteResponse.VoteResponse.builder()
                        .voteId(vote.getId())
                        .candidateCount(Vote.CANDIDATE_COUNT)
                        .createdAt(createdAt)
                        .build();

        return MyActiveVoteResponse.builder()
                .vote(voteResponse)
                .build();
    }

    /**
     * 표 저장 트랜잭션을 끝낸 뒤 최신 투표 현황을 조회한다.
     * 이 메서드 자체에는 트랜잭션을 열지 않아, VoteCommandService의 비관 락이 상세 조회까지 유지되지 않는다.
     */
    public VoteDetailResponse submitBallot(Long userId, Long voteId, SubmitBallotRequest request) {
        User user = userService.getAuthorizedUser(userId);
        voteCommandService.submitBallot(user, voteId, request);
        return voteDetailQueryService.getVoteDetail(voteId, userId);
    }

    /**
     * 투표 종료 트랜잭션을 끝낸 뒤 최신 투표 현황을 조회한다.
     */
    public VoteDetailResponse closeVote(Long userId, Long voteId) {
        User user = userService.getAuthorizedUser(userId);
        voteCommandService.closeVote(user, voteId);
        return voteDetailQueryService.getVoteDetail(voteId, userId);
    }
}
