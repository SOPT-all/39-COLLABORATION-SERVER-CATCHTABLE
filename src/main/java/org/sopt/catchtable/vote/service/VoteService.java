package org.sopt.catchtable.vote.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.service.StoreService;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.service.UserService;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.response.MyActiveVoteResponse;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteRepository voteRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final UserService userService;
    private final StoreService storeService;

    @Transactional
    public CreateVoteResponse createVote(Long userId, CreateVoteRequest request) {
        // DB 조회 없이 할 수 있는 검증을 먼저 해서, 잘못된 요청에는 쿼리를 보내지 않음
        List<Long> storeIds = request.storeIds();
        Vote.validateCandidateStoreIds(storeIds);

        User hostUser = userService.getAuthorizedUser(userId);
        Map<Long, Store> storesById = storeService.getStoresByIds(storeIds);

        // 조회 결과는 순서가 없으므로 요청 순서대로 재정렬
        List<Store> stores = storeIds.stream()
                .map(storesById::get)
                .toList();

        Vote vote = voteRepository.save(Vote.create(hostUser));
        List<VoteCandidate> candidates = vote.createCandidates(stores);
        voteCandidateRepository.saveAll(candidates);

        return CreateVoteResponse.of(vote, candidates);
    }

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
        } else {
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
    }
}
