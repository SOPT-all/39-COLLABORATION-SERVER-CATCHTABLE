package org.sopt.catchtable.vote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 후보 개수·중복은 INVALID_VOTE_CANDIDATES로 응답해야 해서 Bean Validation 대신 Vote 도메인에서 검증
public record CreateVoteRequest(
    @Schema(description = "후보 매장 ID 2개 (요청 순서대로 displayOrder 1, 2)", example = "[1, 2]")
    List<Long> storeIds
) {
}
