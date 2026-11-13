package org.sopt.catchtable.vote.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.SuccessCode;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse;
import org.sopt.catchtable.vote.service.VoteDetailQueryService;
import org.sopt.catchtable.vote.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
@Tag(name = "Vote", description = "투표 API")
public class VoteController {

    private final VoteService voteService;
    private final VoteDetailQueryService voteDetailQueryService;

    @PostMapping
    @Operation(summary = "투표 생성", description = "서로 다른 매장 2개를 후보로 투표를 생성합니다. 요청자가 투표 생성자가 됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "투표 생성 성공"),
        @ApiResponse(responseCode = "400", description = "후보가 서로 다른 매장 2개가 아니거나 요청 형식 오류"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 매장 포함")
    })
    public ResponseEntity<BaseResponse<CreateVoteResponse>> createVote(
        @Parameter(name = "X-User-Id", description = "목 회원 ID", required = true)
        @RequestHeader("X-User-Id") Long userId,
        @RequestBody CreateVoteRequest request
    ) {
        CreateVoteResponse response = voteService.createVote(userId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(BaseResponse.success(SuccessCode.VOTE_CREATED, response));
    }

    @GetMapping("/{voteId}")
    @Operation(
        summary = "투표 상세 조회",
        description = "후보 2개의 매장 비교 정보, 후보별 득표 수와 투표자 닉네임, 요청자의 생성자·투표 여부를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "voteId 형식 오류"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "404", description = "투표를 찾을 수 없음")
    })
    public ResponseEntity<BaseResponse<VoteDetailResponse>> getVoteDetail(
        @Parameter(name = "X-User-Id", description = "목 회원 ID", required = true)
        @RequestHeader("X-User-Id") Long userId,
        @Parameter(description = "투표 ID", required = true)
        @PathVariable Long voteId
    ) {
        VoteDetailResponse response = voteDetailQueryService.getVoteDetail(voteId, userId);
        return ResponseEntity.ok(BaseResponse.success(SuccessCode.SUCCESS, response));
    }
}
