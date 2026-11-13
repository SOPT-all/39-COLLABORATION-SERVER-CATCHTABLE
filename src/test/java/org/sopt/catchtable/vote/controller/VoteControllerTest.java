package org.sopt.catchtable.vote.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.global.exception.GlobalExceptionHandler;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse.CandidateResponse;
import org.sopt.catchtable.vote.service.VoteService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 투표 생성 API의 요청·응답 계약(상태 코드, 공통 응답 형식, 사용자 식별)을 검증한다.
 * 서비스는 mock으로 대체하고, 실제 예외 변환은 GlobalExceptionHandler를 그대로 사용한다.
 */
class VoteControllerTest {

    private static final String CREATE_VOTE_URL = "/api/v1/votes";
    private static final String VALID_BODY = "{\"storeIds\":[3,5]}";

    private VoteService voteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        voteService = mock(VoteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VoteController(voteService))
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
        // when & then
        mockMvc.perform(get(CREATE_VOTE_URL).header("X-User-Id", "1"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
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
}
