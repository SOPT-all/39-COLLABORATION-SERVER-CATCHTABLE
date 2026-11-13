package org.sopt.catchtable.vote.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.response.BaseResponse;
import org.sopt.catchtable.global.response.SuccessCode;
import org.sopt.catchtable.vote.dto.response.MyActiveVoteResponse;
import org.sopt.catchtable.vote.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/votes")
@Tag(name = "Vote", description = "투표 API")
public class MyVoteController {

    private final VoteService voteService;

    @GetMapping("/active")
    @Operation(
            summary = "진행 중인 내 투표 조회",
            description = """
                    요청자가 생성한 OPEN 상태의 투표 중 최신 1건을 조회합니다.
                    생성 시각이 같으면 ID가 큰 투표를 반환합니다.
                    진행 중인 투표가 없으면 data.vote는 null입니다.
                    participantCount와 candidates는 반환하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공. 진행 중인 투표가 없으면 vote는 null"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "X-User-Id 누락·형식 오류 또는 존재하지 않는 사용자"
            )
    })
    public ResponseEntity<BaseResponse<MyActiveVoteResponse>> getMyActiveVote(
            @Parameter(
                    description = "요청 사용자 ID",
                    required = true,
                    example = "1"
            )
            @RequestHeader("X-User-Id") Long userId
    ) {
        MyActiveVoteResponse response =
                voteService.getMyActiveVote(userId);

        return ResponseEntity.ok(
                BaseResponse.success(SuccessCode.SUCCESS, response)
        );
    }
}