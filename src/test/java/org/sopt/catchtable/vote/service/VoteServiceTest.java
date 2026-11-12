package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void 투표와_후보_2개를_요청_순서대로_저장한다() {
        User host = entity(User.class, 1L);
        Store store5 = entity(Store.class, 5L);
        Store store3 = entity(Store.class, 3L);
        given(userService.getAuthorizedUser(1L)).willReturn(host);
        given(storeService.getStoresByIds(List.of(5L, 3L))).willReturn(Map.of(3L, store3, 5L, store5));
        given(voteRepository.save(any(Vote.class))).willAnswer(invocation -> {
            Vote vote = invocation.getArgument(0);
            ReflectionTestUtils.setField(vote, "id", 10L);
            ReflectionTestUtils.setField(vote, "createdAt", LocalDateTime.of(2026, 11, 6, 19, 30, 0, 500_850_000));
            return vote;
        });

        CreateVoteResponse response = voteService.createVote(1L, new CreateVoteRequest(List.of(5L, 3L)));

        assertThat(response.voteId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(VoteStatus.OPEN);
        assertThat(response.hostUserId()).isEqualTo(1L);
        assertThat(response.createdAt().getOffset()).isEqualTo(java.time.ZoneOffset.ofHours(9));
        assertThat(response.createdAt().getNano()).isZero();
        assertThat(response.candidates())
            .extracting(CreateVoteResponse.CandidateResponse::storeId, CreateVoteResponse.CandidateResponse::displayOrder)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(5L, 1),
                org.assertj.core.groups.Tuple.tuple(3L, 2)
            );

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<VoteCandidate>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(voteCandidateRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2)
            .allSatisfy(candidate -> assertThat(candidate.getVote().getId()).isEqualTo(10L));
    }

    @ParameterizedTest
    @MethodSource("invalidStoreIds")
    void 후보가_서로_다른_매장_2개가_아니면_DB_조회_없이_INVALID_VOTE_CANDIDATES(List<Long> storeIds) {
        assertThatThrownBy(() -> voteService.createVote(1L, new CreateVoteRequest(storeIds)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_VOTE_CANDIDATES);

        verifyNoInteractions(userService, storeService, voteRepository, voteCandidateRepository);
    }

    static List<List<Long>> invalidStoreIds() {
        List<List<Long>> cases = new ArrayList<>();
        cases.add(null);                         // 누락
        cases.add(List.of());                    // 0개
        cases.add(List.of(1L));                  // 1개
        cases.add(List.of(1L, 2L, 3L));          // 3개
        cases.add(Arrays.asList(1L, null));      // null 포함
        cases.add(List.of(1L, 1L));              // 중복
        return cases;
    }

    @Test
    void 존재하지_않는_사용자면_UNAUTHORIZED이고_투표를_저장하지_않는다() {
        given(userService.getAuthorizedUser(99L)).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        assertThatThrownBy(() -> voteService.createVote(99L, new CreateVoteRequest(List.of(1L, 2L))))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(voteRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_매장이_있으면_STORE_NOT_FOUND이고_투표를_저장하지_않는다() {
        given(userService.getAuthorizedUser(1L)).willReturn(entity(User.class, 1L));
        given(storeService.getStoresByIds(List.of(1L, 999L))).willThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND));

        assertThatThrownBy(() -> voteService.createVote(1L, new CreateVoteRequest(List.of(1L, 999L))))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORE_NOT_FOUND);

        verify(voteRepository, never()).save(any());
        verify(voteCandidateRepository, never()).saveAll(anyList());
    }

    // protected 기본 생성자로 엔티티를 만들고 id만 채운다
    private static <T> T entity(Class<T> type, Long id) {
        T entity = BeanUtils.instantiateClass(type);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
