package org.sopt.catchtable.vote.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
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
    @Operation(
        summary = "투표 생성",
        description = """
            서로 다른 매장 2개를 후보로 투표를 생성합니다. 요청자(`X-User-Id`)가 투표 생성자가 됩니다.

            **요청 규칙**
            - `Content-Type: application/json`
            - `storeIds`는 서로 다른 매장 ID 2개 (정수). 보낸 순서대로 후보 `displayOrder` 1, 2가 됩니다
            - 확인 순서: 헤더(401) → JSON 형식(400 `INVALID_REQUEST`) → 후보 규칙(400 `INVALID_VOTE_CANDIDATES`) → 매장 존재(404)

            **응답 규칙**
            - 성공 시 `201 Created`, `code`는 `SUCCESS`, `message`는 "투표가 생성되었습니다."
            - `status`는 항상 `OPEN`, `candidates`는 `displayOrder` 오름차순 2개
            - 실패하면 투표·후보 모두 저장되지 않습니다

            **화면 구현 참고**
            - 생성 후 받은 `voteId`로 투표 상세 조회(`GET /api/v1/votes/{voteId}`)를 호출해 화면을 그려 주세요
            - 같은 요청을 두 번 보내면 투표가 2개 생깁니다. 요청 중에는 생성 버튼을 비활성화해 주세요
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "투표 생성 성공"),
        @ApiResponse(responseCode = "400", description = """
            - `INVALID_VOTE_CANDIDATES`: `storeIds` 누락·null, 2개가 아님, null 포함, 같은 매장 중복
            - `INVALID_REQUEST`: body 없음, JSON 형식 오류, 매장 ID가 정수가 아님(1.5 등)
            """,
            content = @Content(mediaType = "application/json", examples = {
                @ExampleObject(name = "후보 규칙 위반", value = """
                    { "success": false, "code": "INVALID_VOTE_CANDIDATES", "message": "서로 다른 매장 2개를 후보로 선택해야 합니다." }
                    """),
                @ExampleObject(name = "요청 형식 오류", value = """
                    { "success": false, "code": "INVALID_REQUEST", "message": "잘못된 요청입니다.", "data": [] }
                    """)
            })),
        @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 없음 / 빈 값 / 숫자 아님 / 존재하지 않는 사용자",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "success": false, "code": "UNAUTHORIZED", "message": "사용자를 확인할 수 없습니다." }
                """))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 매장 포함",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "success": false, "code": "STORE_NOT_FOUND", "message": "매장을 찾을 수 없습니다." }
                """))),
        @ApiResponse(responseCode = "415", description = "Content-Type이 application/json이 아님",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "success": false, "code": "UNSUPPORTED_MEDIA_TYPE", "message": "지원하지 않는 Content-Type입니다." }
                """))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "success": false, "code": "INTERNAL_SERVER_ERROR", "message": "서버 내부 오류입니다." }
                """)))
    })
    public ResponseEntity<BaseResponse<CreateVoteResponse>> createVote(
        @Parameter(name = "X-User-Id", description = "목 회원 ID", required = true, example = "1")
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
        description = """
            후보 2개의 매장 비교 정보, 후보별 득표 수와 투표자 닉네임, 요청자의 생성자·투표 여부를 조회합니다.
            표 제출(API 5)·투표 종료(API 6)도 성공하면 이 API와 같은 구조로 응답합니다.

            **응답 규칙**
            - `candidates`는 `displayOrder` 오름차순, 항상 2개
            - `voters`는 최신 투표순, 표가 없으면 `[]`
            - null 허용 필드(`closedAt`, `viewer.selectedCandidateId`, `viewer.displayName`, `store.aiSummary`)도 키는 항상 포함
            - 시각은 `+09:00`이 붙은 ISO-8601, 초 단위
            - 조회 권한 제한 없음 (`voteId`만 알면 참여하지 않은 사용자도 조회 가능)

            **화면 구현 참고**
            | 화면 요소 | 사용 필드 |
            | --- | --- |
            | 투표 종료 버튼 | `viewer.isHost && status == "OPEN"` |
            | 투표하기 / 결과 보기 분기 | `viewer.hasVoted` |
            | 내가 고른 후보 강조 | `candidateId == viewer.selectedCandidateId` |
            | 닉네임 입력칸 기본값 | `viewer.defaultNickname` |
            | 득표율 | `voteCount / participantCount` (`participantCount`가 0이면 0%) |

            `displayName`, `defaultNickname`은 사용자 입력 그대로 내려오므로 HTML이 아닌 텍스트로 렌더링해 주세요.
            `X-User-Id`가 없거나 형식이 틀리면 401이 가장 먼저이고, 그다음 `voteId` 형식 오류(0 이하 포함) 400, 존재하지 않는 사용자 401 순서로 확인합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "voteId 형식 오류 (abc, 1.5, 공백, 0 이하 등)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "INVALID_REQUEST",
                  "message": "잘못된 요청입니다.",
                  "data": [
                    { "field": "voteId", "message": "요청 값의 형식이 올바르지 않습니다." }
                  ]
                }
                """))),
        @ApiResponse(responseCode = "401", description = "X-User-Id 헤더 없음 / 빈 값 / 숫자 아님 / 존재하지 않는 사용자",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "success": false, "code": "UNAUTHORIZED", "message": "사용자를 확인할 수 없습니다." }
                """))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 투표",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "success": false, "code": "VOTE_NOT_FOUND", "message": "투표를 찾을 수 없습니다." }
                """))),
        @ApiResponse(responseCode = "500", description = "서버 오류 (데이터 정합성 오류 등)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "success": false, "code": "INTERNAL_SERVER_ERROR", "message": "서버 내부 오류입니다." }
                """)))
    })
    public ResponseEntity<BaseResponse<VoteDetailResponse>> getVoteDetail(
        @Parameter(name = "X-User-Id", description = "목 회원 ID", required = true, example = "2")
        @RequestHeader("X-User-Id") Long userId,
        @Parameter(description = "투표 ID (1 이상)", required = true, example = "10")
        @PathVariable @Positive(message = "투표 ID는 1 이상이어야 합니다.") Long voteId
    ) {
        VoteDetailResponse response = voteDetailQueryService.getVoteDetail(voteId, userId);
        return ResponseEntity.ok(BaseResponse.success(SuccessCode.SUCCESS, response));
    }
}
