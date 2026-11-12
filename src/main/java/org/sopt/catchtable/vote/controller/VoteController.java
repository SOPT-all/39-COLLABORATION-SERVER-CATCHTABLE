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
import org.sopt.catchtable.vote.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
