package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.sopt.catchtable.fixture.EntityFixture.candidateWithId;
import static org.sopt.catchtable.fixture.EntityFixture.closedVote;
import static org.sopt.catchtable.fixture.EntityFixture.savedVote;
import static org.sopt.catchtable.fixture.EntityFixture.storeWithId;
import static org.sopt.catchtable.fixture.EntityFixture.userWithId;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.request.SubmitBallotRequest;
import org.sopt.catchtable.vote.repository.BallotRepository;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.dao.PessimisticLockingFailureException;

class VoteCommandServiceTest {

    private VoteRepository voteRepository;
    private VoteCandidateRepository voteCandidateRepository;
    private BallotRepository ballotRepository;
    private VoteCommandService voteCommandService;

    @BeforeEach
    void setUp() {
        voteRepository = mock(VoteRepository.class);
        voteCandidateRepository = mock(VoteCandidateRepository.class);
        ballotRepository = mock(BallotRepository.class);
        voteCommandService = new VoteCommandService(voteRepository, voteCandidateRepository, ballotRepository);
    }

    @DisplayName("진행 중인 투표의 후보를 고르면 공백을 제거한 닉네임으로 표를 저장한다")
    @Test
    void submitBallot() {
        // given
        User host = userWithId(1L);
        User voter = userWithId(2L);
        Vote vote = savedVote(10L, host, LocalDateTime.of(2026, 11, 6, 19, 30));
        VoteCandidate candidate = candidateWithId(21L, vote, storeWithId(3L), 1);
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));
        given(voteCandidateRepository.findByIdAndVoteId(21L, 10L)).willReturn(Optional.of(candidate));
        given(ballotRepository.existsByVoteIdAndUserId(10L, 2L)).willReturn(false);

        // when
        voteCommandService.submitBallot(voter, 10L, new SubmitBallotRequest(21L, " 테스트1 "));

        // then
        ArgumentCaptor<Ballot> ballotCaptor = ArgumentCaptor.forClass(Ballot.class);
        then(ballotRepository).should().save(ballotCaptor.capture());
        assertThat(ballotCaptor.getValue())
            .extracting(Ballot::getVote, Ballot::getUser, Ballot::getCandidate, Ballot::getDisplayName)
            .containsExactly(vote, voter, candidate, "테스트1");
    }

    @DisplayName("종료된 투표에는 표를 제출할 수 없다")
    @Test
    void submitBallotToClosedVote() {
        // given
        Vote vote = closedVote(10L, userWithId(1L), LocalDateTime.now(), LocalDateTime.now());
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));

        // when & then
        assertError(
            () -> voteCommandService.submitBallot(userWithId(2L), 10L, new SubmitBallotRequest(21L, "테스트1")),
            ErrorCode.VOTE_CLOSED
        );

        then(voteCandidateRepository).should(never()).findByIdAndVoteId(any(), any());
        then(ballotRepository).should(never()).save(any());
    }

    @DisplayName("다른 투표의 후보는 선택할 수 없다")
    @Test
    void submitBallotWithForeignCandidate() {
        // given
        Vote vote = savedVote(10L, userWithId(1L), LocalDateTime.now());
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));
        given(voteCandidateRepository.findByIdAndVoteId(21L, 10L)).willReturn(Optional.empty());

        // when & then
        assertError(
            () -> voteCommandService.submitBallot(userWithId(2L), 10L, new SubmitBallotRequest(21L, "테스트1")),
            ErrorCode.INVALID_VOTE_CANDIDATE
        );
    }

    @DisplayName("이미 표를 제출한 사용자는 재투표할 수 없다")
    @Test
    void submitBallotAfterAlreadyVoted() {
        // given
        User voter = userWithId(2L);
        Vote vote = savedVote(10L, userWithId(1L), LocalDateTime.now());
        VoteCandidate candidate = candidateWithId(21L, vote, storeWithId(3L), 1);
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));
        given(voteCandidateRepository.findByIdAndVoteId(21L, 10L)).willReturn(Optional.of(candidate));
        given(ballotRepository.existsByVoteIdAndUserId(10L, 2L)).willReturn(true);

        // when & then
        assertError(
            () -> voteCommandService.submitBallot(voter, 10L, new SubmitBallotRequest(21L, "테스트1")),
            ErrorCode.ALREADY_VOTED
        );

        then(ballotRepository).should(never()).save(any());
    }

    @DisplayName("Vote 행 잠금을 제한 시간 내 얻지 못하면 요청 처리 중으로 안내한다")
    @Test
    void submitBallotWhenVoteLockTimesOut() {
        // given
        given(voteRepository.findByIdForUpdate(10L))
            .willThrow(new PessimisticLockingFailureException("vote lock timeout"));

        // when & then
        assertError(
            () -> voteCommandService.submitBallot(userWithId(2L), 10L, new SubmitBallotRequest(21L, "테스트1")),
            ErrorCode.VOTE_PROCESSING
        );

        then(voteCandidateRepository).should(never()).findByIdAndVoteId(any(), any());
        then(ballotRepository).should(never()).save(any());
    }

    @DisplayName("공백뿐인 닉네임은 거절한다")
    @Test
    void submitBallotWithBlankNickname() {
        // given
        prepareOpenVoteForSubmission();

        // when & then
        assertError(
            () -> voteCommandService.submitBallot(userWithId(2L), 10L, new SubmitBallotRequest(21L, "   ")),
            ErrorCode.INVALID_NICKNAME
        );
    }

    @DisplayName("생성자는 진행 중인 투표를 종료할 수 있다")
    @Test
    void closeVote() {
        // given
        User host = userWithId(1L);
        Vote vote = savedVote(10L, host, LocalDateTime.now());
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));

        // when
        voteCommandService.closeVote(host, 10L);

        // then
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(vote.getClosedAt()).isNotNull();
    }

    @DisplayName("생성자가 아닌 사용자는 투표를 종료할 수 없다")
    @Test
    void closeVoteByNonHost() {
        // given
        Vote vote = savedVote(10L, userWithId(1L), LocalDateTime.now());
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));

        // when & then
        assertError(() -> voteCommandService.closeVote(userWithId(2L), 10L), ErrorCode.VOTE_CLOSE_FORBIDDEN);
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.OPEN);
    }

    @DisplayName("이미 종료된 투표는 다시 종료할 수 없다")
    @Test
    void closeAlreadyClosedVote() {
        // given
        User host = userWithId(1L);
        Vote vote = closedVote(10L, host, LocalDateTime.now(), LocalDateTime.now());
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));

        // when & then
        assertError(() -> voteCommandService.closeVote(host, 10L), ErrorCode.VOTE_ALREADY_CLOSED);
    }

    @DisplayName("투표 종료 중 Vote 행 잠금을 제한 시간 내 얻지 못하면 요청 처리 중으로 안내한다")
    @Test
    void closeVoteWhenVoteLockTimesOut() {
        // given
        given(voteRepository.findByIdForUpdate(10L))
            .willThrow(new PessimisticLockingFailureException("vote lock timeout"));

        // when & then
        assertError(() -> voteCommandService.closeVote(userWithId(1L), 10L), ErrorCode.VOTE_PROCESSING);
    }

    private void prepareOpenVoteForSubmission() {
        Vote vote = savedVote(10L, userWithId(1L), LocalDateTime.now());
        VoteCandidate candidate = candidateWithId(21L, vote, storeWithId(3L), 1);
        given(voteRepository.findByIdForUpdate(10L)).willReturn(Optional.of(vote));
        given(voteCandidateRepository.findByIdAndVoteId(21L, 10L)).willReturn(Optional.of(candidate));
        given(ballotRepository.existsByVoteIdAndUserId(10L, 2L)).willReturn(false);
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(errorCode);
    }
}
