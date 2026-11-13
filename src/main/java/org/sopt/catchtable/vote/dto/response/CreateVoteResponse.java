package org.sopt.catchtable.vote.dto.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;

@Schema(description = "생성된 투표")
public record CreateVoteResponse(
    @Schema(description = "투표 ID. 투표 상세 조회(API 4)에 사용", example = "10", requiredMode = REQUIRED)
    Long voteId,

    @Schema(description = "투표 상태. 생성 직후에는 항상 OPEN", example = "OPEN", requiredMode = REQUIRED)
    VoteStatus status,

    @Schema(description = "투표 생성자 ID (= 요청자 X-User-Id)", example = "1", requiredMode = REQUIRED)
    Long hostUserId,

    @Schema(description = "후보 목록. displayOrder 오름차순, 항상 2개", requiredMode = REQUIRED)
    List<CandidateResponse> candidates,

    @Schema(description = "투표 생성 시각 (+09:00, 초 단위)", example = "2026-11-06T19:30:00+09:00", requiredMode = REQUIRED)
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
            toSeoulOffsetDateTime(vote.getCreatedAt())
        );
    }

    // @CreationTimestamp의 LocalDateTime은 JVM 기본 시간대 기준이므로,
    // 그 시간대로 해석한 뒤 서울 시각으로 바꾼다 (JVM이 UTC여도 응답은 +09:00 기준으로 맞음)
    // 명세 형식(2026-11-06T19:30:00+09:00)에 맞춰 초 단위로 자른다
    private static OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.SECONDS)
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(SEOUL)
            .toOffsetDateTime();
    }

    @Schema(description = "생성된 후보")
    public record CandidateResponse(
        @Schema(description = "후보 ID. 표 제출(API 5)에 사용", example = "21", requiredMode = REQUIRED)
        Long candidateId,

        @Schema(description = "후보 매장 ID", example = "1", requiredMode = REQUIRED)
        Long storeId,

        @Schema(description = "후보 노출 순서 (1, 2). 요청한 storeIds 순서", example = "1", requiredMode = REQUIRED)
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
