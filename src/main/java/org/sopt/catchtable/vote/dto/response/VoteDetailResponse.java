package org.sopt.catchtable.vote.dto.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * 투표 상세 응답. API 4(투표 상세 조회)·5(표 제출)·6(투표 종료)이 같은 구조를 쓴다.
 * 연관 엔티티는 getId()만 읽으므로 지연 로딩 프록시여도 추가 쿼리가 없다.
 */
@Schema(description = "투표 상세. 표 제출(API 5)·투표 종료(API 6)도 같은 구조로 응답한다")
public record VoteDetailResponse(
    @Schema(description = "투표 ID", example = "10", requiredMode = REQUIRED)
    Long voteId,

    @Schema(description = "투표 상태", example = "OPEN", requiredMode = REQUIRED)
    VoteStatus status,

    @Schema(description = "표를 제출한 사람 수. 후보별 voteCount의 합과 같다", example = "2", requiredMode = REQUIRED)
    int participantCount,

    @Schema(description = "후보 목록. displayOrder 오름차순, 항상 2개", requiredMode = REQUIRED)
    List<CandidateDetailResponse> candidates,

    @Schema(description = "요청자 정보", requiredMode = REQUIRED)
    ViewerResponse viewer,

    @Schema(description = "투표 생성 시각 (+09:00, 초 단위)", example = "2026-11-06T19:30:00+09:00", requiredMode = REQUIRED)
    OffsetDateTime createdAt,

    @Schema(description = "투표 종료 시각. 진행 중이면 null", example = "null", nullable = true, requiredMode = REQUIRED)
    OffsetDateTime closedAt
) {

    /**
     * @param candidates       displayOrder 오름차순 후보 (2개가 아니면 IllegalStateException)
     * @param ballots          투표의 모든 표. 이 순서가 투표자 목록 순서가 된다
     * @param storeDetailsById 후보 매장 정보 (하나라도 없으면 IllegalStateException)
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

    // 후보는 항상 2개로 저장되므로, 다르면 데이터 정합성 문제로 보고 500
    private static void validateCandidateCount(Vote vote, List<VoteCandidate> candidates) {
        if (candidates.size() != Vote.CANDIDATE_COUNT) {
            throw new IllegalStateException(
                "투표 후보 수가 올바르지 않습니다. voteId=" + vote.getId() + ", candidateCount=" + candidates.size()
            );
        }
    }

    @Schema(description = "투표 후보")
    public record CandidateDetailResponse(
        @Schema(description = "후보 ID. 표 제출(API 5) 요청에 사용", example = "21", requiredMode = REQUIRED)
        Long candidateId,

        @Schema(description = "후보 노출 순서 (1, 2). 투표 생성 시 보낸 storeIds 순서", example = "1", requiredMode = REQUIRED)
        Integer displayOrder,

        @Schema(description = "후보별 득표 수. 표가 없으면 0", example = "2", requiredMode = REQUIRED)
        int voteCount,

        @Schema(description = "후보를 선택한 투표자 목록. 최신 투표순, 표가 없으면 []", requiredMode = REQUIRED)
        List<VoterResponse> voters,

        @Schema(description = "매장 비교 정보", requiredMode = REQUIRED)
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

        // 후보 매장은 FK로 묶여 있어 항상 있어야 하므로, 없으면 NPE 대신 원인이 드러나는 예외로 500
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

    @Schema(description = "투표자")
    public record VoterResponse(
        @Schema(description = "투표할 때 입력한 닉네임 (최대 15자). 사용자 입력 그대로이므로 텍스트로 렌더링할 것",
            example = "테스트2", requiredMode = REQUIRED)
        String displayName
    ) {

        static VoterResponse from(Ballot ballot) {
            return new VoterResponse(ballot.getDisplayName());
        }
    }

    // 투표 전이면 selectedCandidateId, displayName은 null
    @Schema(description = "요청자(X-User-Id) 기준 정보")
    public record ViewerResponse(
        @Schema(description = "요청자 ID", example = "2", requiredMode = REQUIRED)
        Long userId,

        @Schema(description = "요청자가 투표 생성자인지. 투표 종료 버튼 노출에 사용", example = "false", requiredMode = REQUIRED)
        boolean isHost,

        @Schema(description = "요청자가 투표했는지", example = "true", requiredMode = REQUIRED)
        boolean hasVoted,

        @Schema(description = "요청자가 선택한 후보 ID. 투표 전이면 null", example = "21", nullable = true,
            requiredMode = REQUIRED)
        Long selectedCandidateId,

        @Schema(description = "요청자가 투표할 때 입력한 닉네임. 투표 전이면 null", example = "테스트1", nullable = true,
            requiredMode = REQUIRED)
        String displayName,

        @Schema(description = "요청자의 계정 닉네임. 닉네임 입력칸 기본값으로 사용", example = "테스트1", requiredMode = REQUIRED)
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
