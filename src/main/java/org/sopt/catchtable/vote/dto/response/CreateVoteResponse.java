package org.sopt.catchtable.vote.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;

public record CreateVoteResponse(
    Long voteId,
    VoteStatus status,
    Long hostUserId,
    List<CandidateResponse> candidates,
    OffsetDateTime createdAt
) {

    // 연관 엔티티는 getId()만 사용하므로 지연 로딩 프록시여도 추가 쿼리가 없다
    public static CreateVoteResponse of(Vote vote, List<VoteCandidate> candidates) {
        return new CreateVoteResponse(
            vote.getId(),
            vote.getStatus(),
            vote.getHostUser().getId(),
            candidates.stream().map(CandidateResponse::from).toList(),
            VoteResponseTimes.toSeoulOffsetDateTime(vote.getCreatedAt())
        );
    }

    public record CandidateResponse(
        Long candidateId,
        Long storeId,
        Integer displayOrder
    ) {

        public static CandidateResponse from(VoteCandidate candidate) {
            return new CandidateResponse(
                candidate.getId(),
                candidate.getStore().getId(),
                candidate.getDisplayOrder()
            );
        }
    }
}
