package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.fixture.EntityFixture;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.repository.StoreRepository;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.repository.UserRepository;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.request.SubmitBallotRequest;
import org.sopt.catchtable.vote.repository.BallotRepository;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 각 호출이 별도 스레드·별도 트랜잭션에서 실행되도록 하여 Vote 행 잠금과 DB 유니크 제약을 검증한다.
 * 실제 MySQL을 사용하므로 테스트 실행 전 DB_URL, DB_USERNAME, DB_PASSWORD가 필요하다.
 */
@SpringBootTest
class VoteCommandServiceIntegrationTest {

    private static final long AWAIT_SECONDS = 10;
    private static final long LOCK_HOLD_SECONDS = 5;

    @Autowired
    private VoteCommandService voteCommandService;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteCandidateRepository voteCandidateRepository;

    @Autowired
    private BallotRepository ballotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private final List<Long> candidateIds = new ArrayList<>();
    private final List<Long> voteIds = new ArrayList<>();
    private final List<Long> storeIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        List<Long> persistedBallotIds = voteIds.stream()
            .flatMap(voteId -> ballotRepository.findAllByVoteIdOrderByCreatedAtDescIdDesc(voteId).stream())
            .map(Ballot::getId)
            .toList();
        ballotRepository.deleteAllByIdInBatch(persistedBallotIds);
        voteCandidateRepository.deleteAllByIdInBatch(candidateIds);
        voteRepository.deleteAllByIdInBatch(voteIds);
        storeRepository.deleteAllByIdInBatch(storeIds);
        userRepository.deleteAllByIdInBatch(userIds);
    }

    @DisplayName("같은 사용자의 동시 표 제출은 한 건만 저장되고, 나머지는 ALREADY_VOTED로 거절된다")
    @Test
    void concurrentDuplicateBallotSubmission() throws Exception {
        // given
        User host = saveUser("생성자");
        User voter = saveUser("참여자");
        Vote vote = saveVote(host);
        VoteCandidate candidate = saveCandidate(vote, saveStore("첫 번째 매장"), 1);
        SubmitBallotRequest request = new SubmitBallotRequest(candidate.getId(), "참여자");

        // when
        List<CommandResult> results = runConcurrently(
            () -> submitBallot(voter, vote.getId(), request),
            () -> submitBallot(voter, vote.getId(), request)
        );

        // then
        assertThat(results)
            .extracting(CommandResult::success, CommandResult::errorCode)
            .containsExactlyInAnyOrder(
                tuple(true, null),
                tuple(false, ErrorCode.ALREADY_VOTED)
            );
        assertThat(ballotRepository.findAllByVoteIdOrderByCreatedAtDescIdDesc(vote.getId()))
            .singleElement();
    }

    @DisplayName("표 제출과 투표 종료가 겹쳐도 종료 상태와 최종 표 수가 일관되게 유지된다")
    @Test
    void concurrentBallotSubmissionAndVoteClose() throws Exception {
        // given
        User host = saveUser("생성자");
        User voter = saveUser("참여자");
        Vote vote = saveVote(host);
        VoteCandidate candidate = saveCandidate(vote, saveStore("첫 번째 매장"), 1);
        SubmitBallotRequest request = new SubmitBallotRequest(candidate.getId(), "참여자");

        // when
        List<CommandResult> results = runConcurrently(
            () -> submitBallot(voter, vote.getId(), request),
            () -> closeVote(host, vote.getId())
        );

        // then
        CommandResult submission = results.get(0);
        CommandResult close = results.get(1);
        assertThat(close).isEqualTo(CommandResult.successResult());
        assertThat(submission)
            .isIn(CommandResult.successResult(), CommandResult.failureResult(ErrorCode.VOTE_CLOSED));

        Vote finalVote = voteRepository.findById(vote.getId()).orElseThrow();
        List<Ballot> ballots = ballotRepository.findAllByVoteIdOrderByCreatedAtDescIdDesc(vote.getId());

        assertThat(finalVote.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(finalVote.getClosedAt()).isNotNull();
        assertThat(ballots).hasSize(submission.success() ? 1 : 0);
    }

    @DisplayName("Vote 행 잠금을 3초 내 얻지 못하면 VOTE_PROCESSING으로 거절된다")
    @Test
    void voteLockTimeout() throws Exception {
        // given
        User host = saveUser("생성자");
        User voter = saveUser("참여자");
        Vote vote = saveVote(host);
        VoteCandidate candidate = saveCandidate(vote, saveStore("첫 번째 매장"), 1);
        SubmitBallotRequest request = new SubmitBallotRequest(candidate.getId(), "참여자");
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        ExecutorService lockHolderExecutor = Executors.newSingleThreadExecutor();
        Future<?> lockHolder = lockHolderExecutor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            voteRepository.findByIdForUpdate(vote.getId()).orElseThrow();
            lockAcquired.countDown();
            waitForRelease(releaseLock);
        }));

        try {
            assertThat(lockAcquired.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // when
            CommandResult result = submitBallot(voter, vote.getId(), request);

            // then
            assertThat(result).isEqualTo(CommandResult.failureResult(ErrorCode.VOTE_PROCESSING));
            assertThat(ballotRepository.findAllByVoteIdOrderByCreatedAtDescIdDesc(vote.getId())).isEmpty();
        } finally {
            releaseLock.countDown();
            lockHolder.get(AWAIT_SECONDS, TimeUnit.SECONDS);
            lockHolderExecutor.shutdownNow();
        }
    }

    @SafeVarargs
    private final List<CommandResult> runConcurrently(Callable<CommandResult>... commands) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(commands.length);
        CountDownLatch ready = new CountDownLatch(commands.length);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<CommandResult>> futures = new ArrayList<>();
            for (Callable<CommandResult> command : commands) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(AWAIT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 실행 시작 신호를 기다리다 시간 초과되었습니다.");
                    }
                    return command.call();
                }));
            }

            assertThat(ready.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<CommandResult> results = new ArrayList<>();
            for (Future<CommandResult> future : futures) {
                results.add(future.get(AWAIT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private CommandResult submitBallot(User user, Long voteId, SubmitBallotRequest request) {
        try {
            voteCommandService.submitBallot(user, voteId, request);
            return CommandResult.successResult();
        } catch (BusinessException exception) {
            return CommandResult.failureResult(exception.getErrorCode());
        }
    }

    private CommandResult closeVote(User user, Long voteId) {
        try {
            voteCommandService.closeVote(user, voteId);
            return CommandResult.successResult();
        } catch (BusinessException exception) {
            return CommandResult.failureResult(exception.getErrorCode());
        }
    }

    private void waitForRelease(CountDownLatch latch) {
        try {
            latch.await(LOCK_HOLD_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("잠금 대기 중 인터럽트되었습니다.", exception);
        }
    }

    private User saveUser(String nickname) {
        User user = userRepository.save(EntityFixture.newUser(nickname));
        userIds.add(user.getId());
        return user;
    }

    private Store saveStore(String name) {
        Store store = storeRepository.save(EntityFixture.newStore(name));
        storeIds.add(store.getId());
        return store;
    }

    private Vote saveVote(User host) {
        Vote vote = voteRepository.save(Vote.create(host));
        voteIds.add(vote.getId());
        return vote;
    }

    private VoteCandidate saveCandidate(Vote vote, Store store, int displayOrder) {
        VoteCandidate candidate = voteCandidateRepository.save(VoteCandidate.create(vote, store, displayOrder));
        candidateIds.add(candidate.getId());
        return candidate;
    }

    private record CommandResult(boolean success, ErrorCode errorCode) {

        private static CommandResult successResult() {
            return new CommandResult(true, null);
        }

        private static CommandResult failureResult(ErrorCode errorCode) {
            return new CommandResult(false, errorCode);
        }
    }
}
