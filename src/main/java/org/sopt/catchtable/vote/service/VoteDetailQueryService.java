package org.sopt.catchtable.vote.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse;
import org.sopt.catchtable.store.service.StoreService;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.service.UserService;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse;
import org.sopt.catchtable.vote.repository.BallotRepository;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 현황 조회 (API 4). 표 제출(API 5)·투표 종료(API 6)도 응답을 만들 때 이 조회를 재사용한다.
 *
 * VoteService와 별도 빈으로 둔다.
 * API 5·6은 변경 트랜잭션이 커밋된 뒤 컨트롤러에서 이 메서드를 따로 호출해야 락을 짧게 유지할 수 있다.
 * (@Transactional 메서드 안에서 호출하면 바깥 트랜잭션에 참여해 트랜잭션이 나뉘지 않는다)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteDetailQueryService {

    private final VoteRepository voteRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final BallotRepository ballotRepository;
    private final UserService userService;
    private final StoreService storeService;

    // 후보·표 개수와 관계없이 쿼리 수가 일정하다 (반복문 안에서 조회하지 않음)
    public VoteDetailResponse getVoteDetail(Long voteId, Long viewerUserId) {
        User viewer = userService.getAuthorizedUser(viewerUserId);
        Vote vote = voteRepository.findById(voteId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        List<VoteCandidate> candidates = voteCandidateRepository.findAllByVoteIdOrderByDisplayOrderAsc(voteId);
        List<Ballot> ballots = ballotRepository.findAllByVoteIdOrderByCreatedAtDescIdDesc(voteId);
        Map<Long, StoreDetailResponse> storeDetailsById = getCandidateStores(voteId, candidates);

        return VoteDetailResponse.of(vote, viewer, candidates, ballots, storeDetailsById);
    }

    // 후보 매장은 FK로 묶여 있어 정상이라면 항상 존재한다.
    // 없다면 데이터 정합성 문제이므로 클라이언트 오류(4xx)가 아닌 서버 오류(500)로 남긴다
    private Map<Long, StoreDetailResponse> getCandidateStores(Long voteId, List<VoteCandidate> candidates) {
        List<Long> storeIds = candidates.stream()
            .map(candidate -> candidate.getStore().getId())
            .toList();
        Map<Long, StoreDetailResponse> storeDetailsById = storeService.getStoreDetails(storeIds);

        if (!storeDetailsById.keySet().containsAll(storeIds)) {
            throw new IllegalStateException("후보 매장 정보를 찾을 수 없습니다. voteId=" + voteId);
        }
        return storeDetailsById;
    }
}
