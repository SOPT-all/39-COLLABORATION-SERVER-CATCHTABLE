package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sopt.catchtable.fixture.EntityFixture.storeWithId;
import static org.sopt.catchtable.fixture.EntityFixture.userWithId;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.service.StoreService;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.service.UserService;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.sopt.catchtable.vote.dto.request.SubmitBallotRequest;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse.CandidateResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 투표 생성 규칙을 DB 없이 검증하는 단위 테스트.
 * 실제 저장·롤백은 VoteServiceIntegrationTest에서 확인한다.
 */
class VoteServiceTest {

    private VoteRepository voteRepository;
    private VoteCandidateRepository voteCandidateRepository;
    private UserService userService;
    private StoreService storeService;
    private VoteCommandService voteCommandService;
    private VoteDetailQueryService voteDetailQueryService;
    private VoteService voteService;

    @BeforeEach
    void setUp() {
        voteRepository = mock(VoteRepository.class);
        voteCandidateRepository = mock(VoteCandidateRepository.class);
        userService = mock(UserService.class);
        storeService = mock(StoreService.class);
        voteCommandService = mock(VoteCommandService.class);
        voteDetailQueryService = mock(VoteDetailQueryService.class);
        voteService = new VoteService(
            voteRepository,
            voteCandidateRepository,
            userService,
            storeService,
            voteCommandService,
            voteDetailQueryService
        );
    }

    @DisplayName("서로 다른 매장 2개로 투표를 만들면, 요청한 순서대로 후보 순서가 정해진 진행 중 투표가 만들어진다")
    @Test
    void createVote() {
        // given
        given(userService.getAuthorizedUser(1L)).willReturn(userWithId(1L));
        given(storeService.getStoresByIds(List.of(5L, 3L)))
            .willReturn(Map.of(3L, storeWithId(3L), 5L, storeWithId(5L)));
        givenVoteIsSavedAs(10L, LocalDateTime.of(2026, 11, 6, 19, 30));

        CreateVoteRequest request = new CreateVoteRequest(List.of(5L, 3L));

        // when
        CreateVoteResponse response = voteService.createVote(1L, request);

        // then
        assertThat(response)
            .extracting(CreateVoteResponse::voteId, CreateVoteResponse::status, CreateVoteResponse::hostUserId)
            .containsExactly(10L, VoteStatus.OPEN, 1L);
        assertThat(response.candidates())
            .extracting(CandidateResponse::storeId, CandidateResponse::displayOrder)
            .containsExactly(
                tuple(5L, 1),
                tuple(3L, 2)
            );
        assertThat(response.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }

    @DisplayName("투표를 만들면 두 후보 모두 새로 만든 투표에 속한 채로 저장된다")
    @Test
    void createVoteSavesCandidatesOfTheVote() {
        // given
        given(userService.getAuthorizedUser(1L)).willReturn(userWithId(1L));
        given(storeService.getStoresByIds(List.of(1L, 2L)))
            .willReturn(Map.of(1L, storeWithId(1L), 2L, storeWithId(2L)));
        givenVoteIsSavedAs(10L, LocalDateTime.of(2026, 11, 6, 19, 30));

        // when
        voteService.createVote(1L, new CreateVoteRequest(List.of(1L, 2L)));

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VoteCandidate>> savedCandidates = ArgumentCaptor.forClass(List.class);
        then(voteCandidateRepository).should().saveAll(savedCandidates.capture());
        assertThat(savedCandidates.getValue())
            .hasSize(2)
            .allSatisfy(candidate -> assertThat(candidate.getVote().getId()).isEqualTo(10L));
    }

    // 후보 규칙의 세부 경우는 VoteTest에서 검증하고, 여기서는 "조회 전에 거절"하는 흐름만 확인
    @DisplayName("후보 규칙을 어긴 요청은 사용자나 매장을 조회하지 않고 거절한다")
    @Test
    void createVoteWithInvalidCandidates() {
        // given
        CreateVoteRequest request = new CreateVoteRequest(List.of(1L, 1L));

        // when & then
        assertThatThrownBy(() -> voteService.createVote(1L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_VOTE_CANDIDATES);

        verifyNoInteractions(userService, storeService, voteRepository, voteCandidateRepository);
    }

    @DisplayName("등록되지 않은 사용자는 투표를 만들 수 없다")
    @Test
    void createVoteByUnknownUser() {
        // given
        given(userService.getAuthorizedUser(99L)).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        CreateVoteRequest request = new CreateVoteRequest(List.of(1L, 2L));

        // when & then
        assertThatThrownBy(() -> voteService.createVote(99L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED);

        then(voteRepository).should(never()).save(any());
    }

    @DisplayName("존재하지 않는 매장이 후보에 있으면 투표를 만들 수 없다")
    @Test
    void createVoteWithUnknownStore() {
        // given
        given(userService.getAuthorizedUser(1L)).willReturn(userWithId(1L));
        given(storeService.getStoresByIds(List.of(1L, 999L)))
            .willThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND));
        CreateVoteRequest request = new CreateVoteRequest(List.of(1L, 999L));

        // when & then
        assertThatThrownBy(() -> voteService.createVote(1L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORE_NOT_FOUND);

        then(voteRepository).should(never()).save(any());
        then(voteCandidateRepository).should(never()).saveAll(anyList());
    }

    @DisplayName("표를 저장한 뒤 최신 투표 현황을 반환한다")
    @Test
    void submitBallot() {
        // given
        User user = userWithId(2L);
        SubmitBallotRequest request = new SubmitBallotRequest(21L, "테스트1");
        VoteDetailResponse expected = mock(VoteDetailResponse.class);
        given(userService.getAuthorizedUser(2L)).willReturn(user);
        given(voteDetailQueryService.getVoteDetail(10L, 2L)).willReturn(expected);

        // when
        VoteDetailResponse response = voteService.submitBallot(2L, 10L, request);

        // then
        assertThat(response).isSameAs(expected);
        then(voteCommandService).should().submitBallot(user, 10L, request);
        then(voteDetailQueryService).should().getVoteDetail(10L, 2L);
    }

    @DisplayName("표 제출이 거절되면 상세 조회를 하지 않고 예외를 그대로 전파한다")
    @Test
    void submitBallotFailureDoesNotQueryVoteDetail() {
        // given
        User user = userWithId(2L);
        SubmitBallotRequest request = new SubmitBallotRequest(21L, "테스트1");
        given(userService.getAuthorizedUser(2L)).willReturn(user);
        willThrow(new BusinessException(ErrorCode.VOTE_CLOSED))
            .given(voteCommandService)
            .submitBallot(user, 10L, request);

        // when & then
        assertThatThrownBy(() -> voteService.submitBallot(2L, 10L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VOTE_CLOSED);

        then(voteDetailQueryService).should(never()).getVoteDetail(any(), any());
    }

    @DisplayName("투표를 종료한 뒤 최신 투표 현황을 반환한다")
    @Test
    void closeVote() {
        // given
        User host = userWithId(1L);
        VoteDetailResponse expected = mock(VoteDetailResponse.class);
        given(userService.getAuthorizedUser(1L)).willReturn(host);
        given(voteDetailQueryService.getVoteDetail(10L, 1L)).willReturn(expected);

        // when
        VoteDetailResponse response = voteService.closeVote(1L, 10L);

        // then
        assertThat(response).isSameAs(expected);
        then(voteCommandService).should().closeVote(host, 10L);
        then(voteDetailQueryService).should().getVoteDetail(10L, 1L);
    }

    // 실제 저장처럼 id와 생성 시각을 채워서 돌려준다
    private void givenVoteIsSavedAs(Long voteId, LocalDateTime createdAt) {
        given(voteRepository.save(any(Vote.class))).willAnswer(invocation -> {
            Vote vote = invocation.getArgument(0);
            ReflectionTestUtils.setField(vote, "id", voteId);
            ReflectionTestUtils.setField(vote, "createdAt", createdAt);
            return vote;
        });
    }
}
