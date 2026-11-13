package org.sopt.catchtable.vote.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.global.exception.GlobalExceptionHandler;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse.CandidateResponse;
import org.sopt.catchtable.vote.dto.response.CompareStoreResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse.CandidateDetailResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse.ViewerResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse.VoterResponse;
import org.sopt.catchtable.vote.service.VoteDetailQueryService;
import org.sopt.catchtable.vote.service.VoteService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 투표 API의 요청·응답 계약(상태 코드, 공통 응답 형식, 사용자 식별)을 검증한다.
 * 서비스는 mock으로 대체하고, 실제 예외 변환은 GlobalExceptionHandler를 그대로 사용한다.
 */
class VoteControllerTest {

    private static final String CREATE_VOTE_URL = "/api/v1/votes";
    private static final String VOTE_DETAIL_URL = "/api/v1/votes/{voteId}";
    private static final String VALID_BODY = "{\"storeIds\":[3,5]}";

    private VoteService voteService;
    private VoteDetailQueryService voteDetailQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        voteService = mock(VoteService.class);
        voteDetailQueryService = mock(VoteDetailQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VoteController(voteService, voteDetailQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @DisplayName("투표를 만들면 201 Created와 함께, 생성된 투표와 순서가 정해진 후보 정보를 공통 응답 형식으로 돌려준다")
    @Test
    void createVote() throws Exception {
        // given
        given(voteService.createVote(eq(1L), any())).willReturn(createdVote());

        // when & then
        mockMvc.perform(createVoteRequest().header("X-User-Id", "1").content(VALID_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("투표가 생성되었습니다."))
            .andExpect(jsonPath("$.data.voteId").value(10))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.hostUserId").value(1))
            .andExpect(jsonPath("$.data.candidates[0].storeId").value(3))
            .andExpect(jsonPath("$.data.candidates[0].displayOrder").value(1))
            .andExpect(jsonPath("$.data.candidates[1].storeId").value(5))
            .andExpect(jsonPath("$.data.candidates[1].displayOrder").value(2))
            .andExpect(jsonPath("$.data.createdAt").value("2026-11-06T19:30:00+09:00"));
    }

    @DisplayName("사용자 식별 헤더 없이는 투표를 만들 수 없다")
    @Test
    void createVoteWithoutUserId() throws Exception {
        // given
        MockHttpServletRequestBuilder request = createVoteRequest().content(VALID_BODY);

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        then(voteService).should(never()).createVote(any(), any());
    }

    @DisplayName("사용자 식별 헤더가 숫자가 아니면, 헤더가 없을 때와 같이 투표를 만들 수 없다")
    @Test
    void createVoteWithNonNumericUserId() throws Exception {
        // given
        MockHttpServletRequestBuilder request = createVoteRequest().header("X-User-Id", "abc").content(VALID_BODY);

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        then(voteService).should(never()).createVote(any(), any());
    }

    @DisplayName("사용자 식별 헤더가 비어 있으면, 헤더가 없을 때와 같이 투표를 만들 수 없다")
    @Test
    void createVoteWithBlankUserId() throws Exception {
        // given
        MockHttpServletRequestBuilder request = createVoteRequest().header("X-User-Id", "").content(VALID_BODY);

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        then(voteService).should(never()).createVote(any(), any());
    }

    @DisplayName("요청 본문 없이는 투표를 만들 수 없다")
    @Test
    void createVoteWithoutBody() throws Exception {
        // given
        MockHttpServletRequestBuilder request = createVoteRequest().header("X-User-Id", "1");

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @DisplayName("요청 본문이 JSON null이면 투표를 만들지 않고 잘못된 요청으로 거절된다")
    @Test
    void createVoteWithNullBody() throws Exception {
        // given
        MockHttpServletRequestBuilder request = createVoteRequest().header("X-User-Id", "1").content("null");

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        then(voteService).should(never()).createVote(any(), any());
    }

    @DisplayName("후보 규칙을 어겨 투표 생성이 거절되면, 그 이유를 에러 코드로 알려준다")
    @Test
    void createVoteRejectedByCandidateRule() throws Exception {
        // given
        given(voteService.createVote(eq(1L), any()))
            .willThrow(new BusinessException(ErrorCode.INVALID_VOTE_CANDIDATES));

        // when & then
        mockMvc.perform(createVoteRequest().header("X-User-Id", "1").content("{\"storeIds\":[3,3]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_VOTE_CANDIDATES"));
    }

    @DisplayName("JSON이 아닌 형식으로 보내면 투표를 만들 수 없고, 지원하지 않는 형식이라고 알려준다")
    @Test
    void createVoteWithUnsupportedContentType() throws Exception {
        // given
        MockHttpServletRequestBuilder request = post(CREATE_VOTE_URL)
            .contentType(MediaType.TEXT_PLAIN)
            .header("X-User-Id", "1")
            .content(VALID_BODY);

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        then(voteService).should(never()).createVote(any(), any());
    }

    @DisplayName("투표 생성 주소에 GET으로 요청하면, 지원하지 않는 메서드라고 알려준다")
    @Test
    void getOnCreateVoteUrl() throws Exception {
        // given
        MockHttpServletRequestBuilder request = get(CREATE_VOTE_URL).header("X-User-Id", "1");

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @DisplayName("투표 상세를 조회하면 200 OK와 함께 후보·득표·요청자 정보를 공통 응답 형식으로 돌려준다")
    @Test
    void getVoteDetail() throws Exception {
        // given
        given(voteDetailQueryService.getVoteDetail(10L, 2L)).willReturn(voteDetail());

        // when & then
        mockMvc.perform(get(VOTE_DETAIL_URL, 10).header("X-User-Id", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.data.voteId").value(10))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.participantCount").value(1))
            .andExpect(jsonPath("$.data.candidates[0].candidateId").value(21))
            .andExpect(jsonPath("$.data.candidates[0].voteCount").value(1))
            .andExpect(jsonPath("$.data.candidates[0].voters[0].displayName").value("테스트1"))
            .andExpect(jsonPath("$.data.candidates[0].store.storeId").value(3))
            .andExpect(jsonPath("$.data.candidates[1].voters").isEmpty())
            .andExpect(jsonPath("$.data.viewer.isHost").value(false))
            .andExpect(jsonPath("$.data.viewer.hasVoted").value(true))
            .andExpect(jsonPath("$.data.viewer.selectedCandidateId").value(21))
            .andExpect(jsonPath("$.data.createdAt").value("2026-11-06T19:30:00+09:00"));
    }

    @DisplayName("값이 없는 필드(진행 중 투표의 종료 시각, 투표 전 선택 후보·닉네임)는 빠지지 않고 null로 내려간다")
    @Test
    void getVoteDetailWithNullFields() throws Exception {
        // given
        given(voteDetailQueryService.getVoteDetail(10L, 2L)).willReturn(voteDetailBeforeVoting());

        // when & then
        mockMvc.perform(get(VOTE_DETAIL_URL, 10).header("X-User-Id", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasKey("closedAt")))
            .andExpect(jsonPath("$.data.closedAt").value(nullValue()))
            .andExpect(jsonPath("$.data.viewer", hasKey("selectedCandidateId")))
            .andExpect(jsonPath("$.data.viewer.selectedCandidateId").value(nullValue()))
            .andExpect(jsonPath("$.data.viewer", hasKey("displayName")))
            .andExpect(jsonPath("$.data.viewer.displayName").value(nullValue()));
    }

    @DisplayName("사용자 식별 헤더 없이는 투표 상세를 조회할 수 없다")
    @Test
    void getVoteDetailWithoutUserId() throws Exception {
        // given
        MockHttpServletRequestBuilder request = get(VOTE_DETAIL_URL, 10);

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        then(voteDetailQueryService).should(never()).getVoteDetail(any(), any());
    }

    @DisplayName("투표 ID가 숫자가 아니면 잘못된 요청으로 거절된다")
    @Test
    void getVoteDetailWithNonNumericVoteId() throws Exception {
        // given
        MockHttpServletRequestBuilder request = get(VOTE_DETAIL_URL, "abc").header("X-User-Id", "2");

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        then(voteDetailQueryService).should(never()).getVoteDetail(any(), any());
    }

    @DisplayName("투표 ID가 공백이면 서버 오류가 아니라 잘못된 요청으로 거절된다")
    @Test
    void getVoteDetailWithBlankVoteId() throws Exception {
        // given
        MockHttpServletRequestBuilder request = get("/api/v1/votes/%20").header("X-User-Id", "2");

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.data[0].field").value("voteId"));

        then(voteDetailQueryService).should(never()).getVoteDetail(any(), any());
    }

    @DisplayName("투표 ID가 0 이하이면 조회하지 않고, 1 이상이어야 한다는 잘못된 요청으로 거절된다")
    @ParameterizedTest(name = "voteId = {0}")
    @ValueSource(strings = {"0", "-1"})
    void getVoteDetailWithNonPositiveVoteId(String voteId) throws Exception {
        // given
        MockHttpServletRequestBuilder request = get(VOTE_DETAIL_URL, voteId).header("X-User-Id", "2");

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.data[0].field").value("voteId"))
            .andExpect(jsonPath("$.data[0].message").value("투표 ID는 1 이상이어야 합니다."));

        then(voteDetailQueryService).should(never()).getVoteDetail(any(), any());
    }

    @DisplayName("없는 투표를 조회하면 404와 함께 투표를 찾을 수 없다고 알려준다")
    @Test
    void getUnknownVoteDetail() throws Exception {
        // given
        given(voteDetailQueryService.getVoteDetail(999L, 2L))
            .willThrow(new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        // when & then
        mockMvc.perform(get(VOTE_DETAIL_URL, 999).header("X-User-Id", "2"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("VOTE_NOT_FOUND"));
    }

    @DisplayName("표를 제출하면 최신 투표 현황을 공통 성공 응답으로 반환한다")
    @Test
    void submitBallot() throws Exception {
        // given
        given(voteService.submitBallot(eq(2L), eq(10L), any())).willReturn(voteDetail());

        // when & then
        mockMvc.perform(post("/api/v1/votes/10/ballots/me")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"candidateId\":21,\"nickname\":\"테스트1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.voteId").value(10))
            .andExpect(jsonPath("$.data.viewer.selectedCandidateId").value(21));
    }

    @DisplayName("후보 ID가 없으면 표를 제출하지 않고 잘못된 요청으로 거절한다")
    @Test
    void submitBallotWithoutCandidateId() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/votes/10/ballots/me")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"테스트1\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        then(voteService).should(never()).submitBallot(any(), any(), any());
    }

    @DisplayName("이미 투표한 사용자가 다시 표를 제출하면 재투표가 아니라 충돌로 거절된다")
    @Test
    void submitBallotAfterAlreadyVoted() throws Exception {
        // given
        given(voteService.submitBallot(eq(2L), eq(10L), any()))
            .willThrow(new BusinessException(ErrorCode.ALREADY_VOTED));

        // when & then
        mockMvc.perform(post("/api/v1/votes/10/ballots/me")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"candidateId\":21,\"nickname\":\"테스트1\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("ALREADY_VOTED"));
    }

    @DisplayName("같은 투표의 다른 요청을 처리 중이면 표 제출을 충돌로 안내한다")
    @Test
    void submitBallotWhileVoteIsProcessing() throws Exception {
        // given
        given(voteService.submitBallot(eq(2L), eq(10L), any()))
            .willThrow(new BusinessException(ErrorCode.VOTE_PROCESSING));

        // when & then
        mockMvc.perform(post("/api/v1/votes/10/ballots/me")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"candidateId\":21,\"nickname\":\"테스트1\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VOTE_PROCESSING"));
    }

    @DisplayName("생성자가 투표를 종료하면 종료 메시지와 최신 투표 현황을 반환한다")
    @Test
    void closeVote() throws Exception {
        // given
        given(voteService.closeVote(1L, 10L)).willReturn(closedVoteDetail());

        // when & then
        mockMvc.perform(patch("/api/v1/votes/10/close").header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("투표가 종료되었습니다."))
            .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @DisplayName("이미 종료된 투표를 다시 종료하면 충돌로 거절된다")
    @Test
    void closeVoteAfterAlreadyClosed() throws Exception {
        // given
        given(voteService.closeVote(1L, 10L))
            .willThrow(new BusinessException(ErrorCode.VOTE_ALREADY_CLOSED));

        // when & then
        mockMvc.perform(patch("/api/v1/votes/10/close").header("X-User-Id", "1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VOTE_ALREADY_CLOSED"));
    }

    @DisplayName("같은 투표의 다른 요청을 처리 중이면 투표 종료를 충돌로 안내한다")
    @Test
    void closeVoteWhileVoteIsProcessing() throws Exception {
        // given
        given(voteService.closeVote(1L, 10L))
            .willThrow(new BusinessException(ErrorCode.VOTE_PROCESSING));

        // when & then
        mockMvc.perform(patch("/api/v1/votes/10/close").header("X-User-Id", "1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VOTE_PROCESSING"));
    }

    private MockHttpServletRequestBuilder createVoteRequest() {
        return post(CREATE_VOTE_URL).contentType(MediaType.APPLICATION_JSON);
    }

    private CreateVoteResponse createdVote() {
        return new CreateVoteResponse(
            10L,
            VoteStatus.OPEN,
            1L,
            List.of(
                new CandidateResponse(21L, 3L, 1),
                new CandidateResponse(22L, 5L, 2)
            ),
            OffsetDateTime.of(2026, 11, 6, 19, 30, 0, 0, ZoneOffset.ofHours(9))
        );
    }

    private VoteDetailResponse voteDetail() {
        return new VoteDetailResponse(
            10L,
            VoteStatus.OPEN,
            1,
            List.of(
                new CandidateDetailResponse(21L, 1, 1, List.of(new VoterResponse("테스트1")), compareStore(3L)),
                new CandidateDetailResponse(22L, 2, 0, List.of(), compareStore(5L))
            ),
            new ViewerResponse(2L, false, true, 21L, "테스트1", "테스트1"),
            OffsetDateTime.of(2026, 11, 6, 19, 30, 0, 0, ZoneOffset.ofHours(9)),
            null
        );
    }

    private VoteDetailResponse voteDetailBeforeVoting() {
        return new VoteDetailResponse(
            10L,
            VoteStatus.OPEN,
            0,
            List.of(
                new CandidateDetailResponse(21L, 1, 0, List.of(), compareStore(3L)),
                new CandidateDetailResponse(22L, 2, 0, List.of(), compareStore(5L))
            ),
            new ViewerResponse(2L, false, false, null, null, "테스트2"),
            OffsetDateTime.of(2026, 11, 6, 19, 30, 0, 0, ZoneOffset.ofHours(9)),
            null
        );
    }

    private VoteDetailResponse closedVoteDetail() {
        return new VoteDetailResponse(
            10L,
            VoteStatus.CLOSED,
            1,
            List.of(
                new CandidateDetailResponse(21L, 1, 1, List.of(new VoterResponse("테스트1")), compareStore(3L)),
                new CandidateDetailResponse(22L, 2, 0, List.of(), compareStore(5L))
            ),
            new ViewerResponse(1L, true, false, null, null, "캐치러버"),
            OffsetDateTime.of(2026, 11, 6, 19, 30, 0, 0, ZoneOffset.ofHours(9)),
            OffsetDateTime.of(2026, 11, 6, 20, 10, 0, 0, ZoneOffset.ofHours(9))
        );
    }

    private CompareStoreResponse compareStore(Long storeId) {
        return new CompareStoreResponse(storeId, "매장" + storeId, null, null, List.of(), null, null, null, List.of(), List.of());
    }
}
