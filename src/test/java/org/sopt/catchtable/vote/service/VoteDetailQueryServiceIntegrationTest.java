package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.fixture.EntityFixture;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.repository.StoreRepository;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.repository.UserRepository;
import org.sopt.catchtable.vote.domain.Ballot;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse.CandidateDetailResponse;
import org.sopt.catchtable.vote.dto.response.VoteDetailResponse.VoterResponse;
import org.sopt.catchtable.vote.repository.BallotRepository;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 DB의 투표·후보·표로 투표 상세 응답이 올바르게 조립되는지 검증하는 통합 테스트.
 *
 * 트랜잭션 밖에서 호출해도 지연 로딩 오류가 없는지 확인하려고 @Transactional을 붙이지 않는다.
 * 대신 테스트가 만든 데이터만 골라서 지운다 (외래키 때문에 자식 테이블부터).
 */
@SpringBootTest
class VoteDetailQueryServiceIntegrationTest {

    @Autowired
    private VoteDetailQueryService voteDetailQueryService;

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

    private final List<Long> ballotIds = new ArrayList<>();
    private final List<Long> candidateIds = new ArrayList<>();
    private final List<Long> voteIds = new ArrayList<>();
    private final List<Long> storeIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        ballotRepository.deleteAllByIdInBatch(ballotIds);
        voteCandidateRepository.deleteAllByIdInBatch(candidateIds);
        voteRepository.deleteAllByIdInBatch(voteIds);
        storeRepository.deleteAllByIdInBatch(storeIds);
        userRepository.deleteAllByIdInBatch(userIds);
    }

    @DisplayName("저장된 표를 후보별로 모아, 득표 수와 최신순 투표자 목록, 요청자의 선택을 함께 돌려준다")
    @Test
    void getVoteDetail() {
        // given
        User host = saveUser("생성자");
        User voterA = saveUser("계정A");
        User voterB = saveUser("계정B");
        Store firstStore = saveStore("첫 번째 매장");
        Store secondStore = saveStore("두 번째 매장");

        Vote vote = saveVote(host);
        VoteCandidate first = saveCandidate(vote, firstStore, 1);
        VoteCandidate second = saveCandidate(vote, secondStore, 2);

        saveBallot(vote, voterA, first, "먼저 투표");
        saveBallot(vote, voterB, first, "나중 투표");

        // when
        VoteDetailResponse response = voteDetailQueryService.getVoteDetail(vote.getId(), voterA.getId());

        // then
        assertThat(response.voteId()).isEqualTo(vote.getId());
        assertThat(response.status()).isEqualTo(VoteStatus.OPEN);
        assertThat(response.participantCount()).isEqualTo(2);
        assertThat(response.closedAt()).isNull();

        assertThat(response.candidates())
            .extracting(
                CandidateDetailResponse::candidateId,
                CandidateDetailResponse::displayOrder,
                CandidateDetailResponse::voteCount,
                CandidateDetailResponse::voters,
                candidate -> candidate.store().name()
            )
            .containsExactly(
                tuple(first.getId(), 1, 2,
                    List.of(new VoterResponse("나중 투표"), new VoterResponse("먼저 투표")), "첫 번째 매장"),
                tuple(second.getId(), 2, 0, List.of(), "두 번째 매장")
            );

        assertThat(response.viewer().isHost()).isFalse();
        assertThat(response.viewer().hasVoted()).isTrue();
        assertThat(response.viewer().selectedCandidateId()).isEqualTo(first.getId());
        assertThat(response.viewer().displayName()).isEqualTo("먼저 투표");
        assertThat(response.viewer().defaultNickname()).isEqualTo("계정A");
    }

    @DisplayName("투표를 만든 사람이 아직 투표하지 않았다면, 생성자로 표시되고 선택한 후보는 없다")
    @Test
    void getVoteDetailByHostBeforeVoting() {
        // given
        User host = saveUser("생성자");
        Vote vote = saveVote(host);
        saveCandidate(vote, saveStore("첫 번째 매장"), 1);
        saveCandidate(vote, saveStore("두 번째 매장"), 2);

        // when
        VoteDetailResponse response = voteDetailQueryService.getVoteDetail(vote.getId(), host.getId());

        // then
        assertThat(response.participantCount()).isZero();
        assertThat(response.viewer().isHost()).isTrue();
        assertThat(response.viewer().hasVoted()).isFalse();
        assertThat(response.viewer().selectedCandidateId()).isNull();
        assertThat(response.viewer().defaultNickname()).isEqualTo("생성자");
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

    private Ballot saveBallot(Vote vote, User user, VoteCandidate candidate, String displayName) {
        Ballot ballot = ballotRepository.save(Ballot.create(vote, user, candidate, displayName));
        ballotIds.add(ballot.getId());
        return ballot;
    }
}
