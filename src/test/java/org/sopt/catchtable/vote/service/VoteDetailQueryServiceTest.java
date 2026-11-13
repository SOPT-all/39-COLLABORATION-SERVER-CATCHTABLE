package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.sopt.catchtable.fixture.EntityFixture.ballotWithId;
import static org.sopt.catchtable.fixture.EntityFixture.candidateWithId;
import static org.sopt.catchtable.fixture.EntityFixture.savedVote;
import static org.sopt.catchtable.fixture.EntityFixture.storeDetail;
import static org.sopt.catchtable.fixture.EntityFixture.storeWithId;
import static org.sopt.catchtable.fixture.EntityFixture.userWithId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.service.StoreService;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.service.UserService;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse;
import org.sopt.catchtable.vote.repository.BallotRepository;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;

/**
 * 투표 현황 조회의 흐름(요청자 확인 → 투표 조회 → 후보·표·매장 조회)을 DB 없이 검증한다.
 */
class VoteDetailQueryServiceTest {

    private static final Long VOTE_ID = 10L;

    private VoteRepository voteRepository;
    private VoteCandidateRepository voteCandidateRepository;
    private BallotRepository ballotRepository;
    private UserService userService;
    private StoreService storeService;
    private VoteDetailQueryService voteDetailQueryService;

    private User host;
    private Vote vote;
    private VoteCandidate first;
    private VoteCandidate second;

    @BeforeEach
    void setUp() {
        voteRepository = mock(VoteRepository.class);
        voteCandidateRepository = mock(VoteCandidateRepository.class);
        ballotRepository = mock(BallotRepository.class);
        userService = mock(UserService.class);
        storeService = mock(StoreService.class);
        voteDetailQueryService = new VoteDetailQueryService(
            voteRepository, voteCandidateRepository, ballotRepository, userService, storeService
        );

        host = userWithId(1L);
        vote = savedVote(VOTE_ID, host, LocalDateTime.of(2026, 11, 6, 19, 30));
        first = candidateWithId(21L, vote, storeWithId(3L), 1);
        second = candidateWithId(22L, vote, storeWithId(5L), 2);
    }

    @DisplayName("투표 현황을 조회하면 후보 매장 정보를 한 번에 가져와 후보·득표·요청자 정보를 함께 돌려준다")
    @Test
    void getVoteDetail() {
        // given
        User viewer = userWithId(2L);
        given(userService.getAuthorizedUser(2L)).willReturn(viewer);
        given(voteRepository.findById(VOTE_ID)).willReturn(Optional.of(vote));
        given(voteCandidateRepository.findAllByVoteIdOrderByDisplayOrderAsc(VOTE_ID)).willReturn(List.of(first, second));
        given(ballotRepository.findAllByVoteIdOrderByCreatedAtDescIdDesc(VOTE_ID))
            .willReturn(List.of(ballotWithId(101L, vote, viewer, first, "투표닉네임")));
        given(storeService.getStoreDetails(List.of(3L, 5L)))
            .willReturn(Map.of(3L, storeDetail(3L), 5L, storeDetail(5L)));

        // when
        VoteDetailResponse response = voteDetailQueryService.getVoteDetail(VOTE_ID, 2L);

        // then
        assertThat(response.voteId()).isEqualTo(VOTE_ID);
        assertThat(response.participantCount()).isEqualTo(1);
        assertThat(response.candidates()).hasSize(2);
        assertThat(response.viewer().selectedCandidateId()).isEqualTo(21L);

        then(storeService).should().getStoreDetails(List.of(3L, 5L));
    }

    @DisplayName("확인할 수 없는 사용자는 투표가 있는지조차 조회하지 않고 거절된다")
    @Test
    void getVoteDetailByUnknownUser() {
        // given
        given(userService.getAuthorizedUser(999L)).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> voteDetailQueryService.getVoteDetail(VOTE_ID, 999L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED);

        then(voteRepository).should(never()).findById(any());
    }

    @DisplayName("없는 투표를 조회하면 투표를 찾을 수 없다고 알려주고, 후보·표는 조회하지 않는다")
    @Test
    void getUnknownVote() {
        // given
        given(userService.getAuthorizedUser(1L)).willReturn(host);
        given(voteRepository.findById(VOTE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> voteDetailQueryService.getVoteDetail(VOTE_ID, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VOTE_NOT_FOUND);

        then(voteCandidateRepository).should(never()).findAllByVoteIdOrderByDisplayOrderAsc(anyLong());
        then(ballotRepository).should(never()).findAllByVoteIdOrderByCreatedAtDescIdDesc(anyLong());
    }

    @DisplayName("후보 매장 정보가 비어 있으면 클라이언트 잘못이 아니므로 서버 오류로 처리한다")
    @Test
    void candidateStoreMissing() {
        // given
        given(userService.getAuthorizedUser(1L)).willReturn(host);
        given(voteRepository.findById(VOTE_ID)).willReturn(Optional.of(vote));
        given(voteCandidateRepository.findAllByVoteIdOrderByDisplayOrderAsc(VOTE_ID)).willReturn(List.of(first, second));
        given(ballotRepository.findAllByVoteIdOrderByCreatedAtDescIdDesc(VOTE_ID)).willReturn(List.of());
        given(storeService.getStoreDetails(List.of(3L, 5L))).willReturn(Map.of(3L, storeDetail(3L)));

        // when & then
        assertThatThrownBy(() -> voteDetailQueryService.getVoteDetail(VOTE_ID, 1L))
            .isInstanceOf(IllegalStateException.class);
    }
}
