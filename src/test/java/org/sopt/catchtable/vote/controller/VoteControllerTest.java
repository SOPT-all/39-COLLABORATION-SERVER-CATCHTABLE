package org.sopt.catchtable.vote.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

class VoteControllerTest {

    private VoteService voteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        voteService = mock(VoteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VoteController(voteService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void 투표_생성_성공시_201과_생성된_투표를_반환한다() throws Exception {
        given(voteService.createVote(eq(1L), any())).willReturn(createVoteResponse());

        mockMvc.perform(post("/api/v1/votes")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeIds\":[3,5]}"))
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

    @Test
    void X_User_Id_헤더가_없으면_401_UNAUTHORIZED를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeIds\":[3,5]}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(voteService, never()).createVote(any(), any());
    }

    @Test
    void X_User_Id가_숫자가_아니면_401_UNAUTHORIZED를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/votes")
                .header("X-User-Id", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeIds\":[3,5]}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(voteService, never()).createVote(any(), any());
    }

    @Test
    void X_User_Id가_비어_있으면_401_UNAUTHORIZED를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/votes")
                .header("X-User-Id", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeIds\":[3,5]}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 요청_본문이_없으면_400_INVALID_REQUEST를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/votes")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 서비스의_비즈니스_예외는_해당_에러_코드로_응답한다() throws Exception {
        given(voteService.createVote(eq(1L), any()))
            .willThrow(new BusinessException(ErrorCode.INVALID_VOTE_CANDIDATES));

        mockMvc.perform(post("/api/v1/votes")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeIds\":[3,3]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_VOTE_CANDIDATES"));
    }

    private CreateVoteResponse createVoteResponse() {
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
