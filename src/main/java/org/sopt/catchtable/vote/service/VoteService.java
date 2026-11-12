package org.sopt.catchtable.vote.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.service.StoreService;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.service.UserService;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private static final int CANDIDATE_COUNT = 2;

    private final VoteRepository voteRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final UserService userService;
    private final StoreService storeService;

    @Transactional
    public CreateVoteResponse createVote(Long userId, CreateVoteRequest request) {
        // DB 조회가 필요 없는 검증을 먼저 해서 잘못된 요청에는 쿼리를 보내지 않는다
        List<Long> storeIds = request.storeIds();
        validateCandidates(storeIds);

        User hostUser = userService.getAuthorizedUser(userId);
        Map<Long, Store> stores = storeService.getStoresByIds(storeIds);

        Vote vote = voteRepository.save(Vote.create(hostUser));

        // 요청 순서대로 displayOrder 1, 2 부여
        List<VoteCandidate> candidates = IntStream.range(0, storeIds.size())
            .mapToObj(i -> VoteCandidate.create(vote, stores.get(storeIds.get(i)), i + 1))
            .toList();
        voteCandidateRepository.saveAll(candidates);

        return CreateVoteResponse.of(vote, candidates);
    }

    private void validateCandidates(List<Long> storeIds) {
        if (storeIds == null
            || storeIds.size() != CANDIDATE_COUNT
            || storeIds.stream().anyMatch(Objects::isNull)
            || new HashSet<>(storeIds).size() != CANDIDATE_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_VOTE_CANDIDATES);
        }
    }
}
