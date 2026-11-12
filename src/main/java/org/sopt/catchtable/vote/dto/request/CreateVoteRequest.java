package org.sopt.catchtable.vote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 후보 개수·중복 검증은 INVALID_VOTE_CANDIDATES 코드로 응답해야 하므로 Bean Validation 대신 서비스에서 처리
public record CreateVoteRequest(
    @Schema(description = "후보 매장 ID 2개 (요청 순서대로 displayOrder 1, 2)", example = "[1, 2]")
    List<Long> storeIds
) {
}
