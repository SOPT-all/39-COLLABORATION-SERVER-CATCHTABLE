package org.sopt.catchtable.vote.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 후보 개수·중복은 INVALID_VOTE_CANDIDATES로 응답해야 해서 Bean Validation 대신 Vote 도메인에서 검증
@Schema(description = "투표 생성 요청", example = "{\"storeIds\": [1, 2]}")
public record CreateVoteRequest(
    @ArraySchema(
        arraySchema = @Schema(
            description = "후보 매장 ID. 서로 다른 매장 2개, 보낸 순서대로 displayOrder 1, 2가 된다",
            requiredMode = Schema.RequiredMode.REQUIRED
        ),
        schema = @Schema(description = "매장 ID (정수)", example = "1"),
        minItems = 2,
        maxItems = 2,
        uniqueItems = true
    )
    List<Long> storeIds
) {
}
