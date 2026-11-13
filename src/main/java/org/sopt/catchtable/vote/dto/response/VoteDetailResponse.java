package org.sopt.catchtable.vote.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;

/**
 * 투표 현황 응답. API 4(투표 현황 조회), 5(표 제출), 6(투표 종료)이 같은 구조를 쓴다.
 * 연관 엔티티는 getId()만 사용하므로 지연 로딩 프록시여도 추가 쿼리가 없다.
 */
public record VoteDetailResponse(
    Long voteId,
    VoteStatus status,
    int participantCount,
    List<CandidateDetailResponse> candidates,
    ViewerResponse viewer,
    OffsetDateTime createdAt,
    OffsetDateTime closedAt
) {

    /**
     * @param candidates       displayOrder 오름차순으로 정렬된 후보. 개수가 Vote.CANDIDATE_COUNT가 아니면 IllegalStateException
     * @param ballots          해당 투표의 모든 표. 이 순서가 후보별 투표자 순서가 된다
     * @param storeDetailsById 후보 매장 ID별 매장 정보. 후보 매장이 하나라도 없으면 IllegalStateException
     */
    public static VoteDetailResponse of(
        Vote vote,
        User viewer,
        List<VoteCandidate> candidates,
        List<Ballot> ballots,
        Map<Long, StoreDetailResponse> storeDetailsById
    ) {
        validateCandidateCount(vote, candidates);

        Map<Long, List<Ballot>> ballotsByCandidateId = ballots.stream()
            .collect(Collectors.groupingBy(ballot -> ballot.getCandidate().getId()));

        Ballot viewerBallot = ballots.stream()
            .filter(ballot -> Objects.equals(ballot.getUser().getId(), viewer.getId()))
            .findFirst()
            .orElse(null);

        return new VoteDetailResponse(
            vote.getId(),
            vote.getStatus(),
            ballots.size(),
            candidates.stream()
                .map(candidate -> CandidateDetailResponse.of(
                    candidate,
                    ballotsByCandidateId.getOrDefault(candidate.getId(), List.of()),
                    storeDetailsById
                ))
                .toList(),
            ViewerResponse.of(vote, viewer, viewerBallot),
            VoteResponseTimes.toSeoulOffsetDateTime(vote.getCreatedAt()),
            VoteResponseTimes.toSeoulOffsetDateTime(vote.getClosedAt())
        );
    }

    // 투표 후보는 생성 시 항상 2개로 저장된다 (Vote.CANDIDATE_COUNT).
    // 다르다면 데이터 정합성 문제이므로 명세("항상 2개")와 다른 응답을 200으로 내보내지 않고 실패한다 (500)
    private static void validateCandidateCount(Vote vote, List<VoteCandidate> candidates) {
        if (candidates.size() != Vote.CANDIDATE_COUNT) {
            throw new IllegalStateException(
                "투표 후보 수가 올바르지 않습니다. voteId=" + vote.getId() + ", candidateCount=" + candidates.size()
            );
        }
    }

    public record CandidateDetailResponse(
        Long candidateId,
        Integer displayOrder,
        int voteCount,
        List<VoterResponse> voters,
        CompareStoreResponse store
    ) {

        static CandidateDetailResponse of(
            VoteCandidate candidate,
            List<Ballot> ballots,
            Map<Long, StoreDetailResponse> storeDetailsById
        ) {
            StoreDetailResponse store = getCandidateStore(candidate, storeDetailsById);
            return new CandidateDetailResponse(
                candidate.getId(),
                candidate.getDisplayOrder(),
                ballots.size(),
                ballots.stream().map(VoterResponse::from).toList(),
                CompareStoreResponse.from(store)
            );
        }

        // 후보 매장은 FK로 묶여 있어 정상이라면 항상 존재한다.
        // 없다면 데이터 정합성 문제(또는 매장 조회 누락)이므로 NPE 대신 원인이 드러나는 예외로 실패한다 (500)
        private static StoreDetailResponse getCandidateStore(
            VoteCandidate candidate,
            Map<Long, StoreDetailResponse> storeDetailsById
        ) {
            Long storeId = candidate.getStore().getId();
            StoreDetailResponse store = storeDetailsById.get(storeId);
            if (store == null) {
                throw new IllegalStateException(
                    "후보 매장 정보를 찾을 수 없습니다. candidateId=" + candidate.getId() + ", storeId=" + storeId
                );
            }
            return store;
        }
    }

    public record VoterResponse(String displayName) {

        static VoterResponse from(Ballot ballot) {
            return new VoterResponse(ballot.getDisplayName());
        }
    }

    // 투표 전이면 selectedCandidateId, displayName은 null
    public record ViewerResponse(
        Long userId,
        boolean isHost,
        boolean hasVoted,
        Long selectedCandidateId,
        String displayName,
        String defaultNickname
    ) {

        static ViewerResponse of(Vote vote, User viewer, Ballot viewerBallot) {
            boolean hasVoted = viewerBallot != null;
            return new ViewerResponse(
                viewer.getId(),
                vote.isHostedBy(viewer.getId()),
                hasVoted,
                hasVoted ? viewerBallot.getCandidate().getId() : null,
                hasVoted ? viewerBallot.getDisplayName() : null,
                viewer.getNickname()
            );
        }
    }
}
