package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sopt.catchtable.fixture.EntityFixture.storeWithId;
import static org.sopt.catchtable.fixture.EntityFixture.userWithId;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.service.StoreService;
import org.sopt.catchtable.user.service.UserService;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse.CandidateResponse;
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
    private VoteService voteService;

    @BeforeEach
    void setUp() {
        voteRepository = mock(VoteRepository.class);
        voteCandidateRepository = mock(VoteCandidateRepository.class);
        userService = mock(UserService.class);
        storeService = mock(StoreService.class);
        voteService = new VoteService(voteRepository, voteCandidateRepository, userService, storeService);
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

    @DisplayName("후보가 서로 다른 매장 2개가 아니면, 사용자나 매장을 조회하지 않고 투표 생성을 거절한다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("candidateRuleViolations")
    void createVoteWithInvalidCandidates(String situation, List<Long> storeIds) {
        // given
        CreateVoteRequest request = new CreateVoteRequest(storeIds);

        // when & then
        assertThatThrownBy(() -> voteService.createVote(1L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_VOTE_CANDIDATES);

        verifyNoInteractions(userService, storeService, voteRepository, voteCandidateRepository);
    }

    static Stream<Arguments> candidateRuleViolations() {
        return Stream.of(
            Arguments.of("후보 목록을 보내지 않은 경우", null),
            Arguments.of("후보를 하나도 고르지 않은 경우", List.of()),
            Arguments.of("후보를 1개만 고른 경우", List.of(1L)),
            Arguments.of("후보를 3개 고른 경우", List.of(1L, 2L, 3L)),
            Arguments.of("후보에 빈 값이 섞인 경우", Arrays.asList(1L, null)),
            Arguments.of("같은 매장을 두 번 고른 경우", List.of(1L, 1L))
        );
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
