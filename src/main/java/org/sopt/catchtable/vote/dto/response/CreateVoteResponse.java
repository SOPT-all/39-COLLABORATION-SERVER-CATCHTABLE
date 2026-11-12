package org.sopt.catchtable.vote.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 연관 엔티티는 getId()만 사용하므로 지연 로딩 프록시여도 추가 쿼리가 없다
    public static CreateVoteResponse of(Vote vote, List<VoteCandidate> candidates) {
        return new CreateVoteResponse(
            vote.getId(),
            vote.getStatus(),
            vote.getHostUser().getId(),
            candidates.stream().map(CandidateResponse::from).toList(),
            // 명세 형식(2026-11-06T19:30:00+09:00)에 맞춰 초 단위로 자른다
            vote.getCreatedAt().truncatedTo(ChronoUnit.SECONDS).atZone(SEOUL).toOffsetDateTime()
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
