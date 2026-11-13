package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.fixture.EntityFixture;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.repository.StoreRepository;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.user.repository.UserRepository;
import org.sopt.catchtable.vote.domain.Vote;
import org.sopt.catchtable.vote.domain.VoteCandidate;
import org.sopt.catchtable.vote.domain.VoteStatus;
import org.sopt.catchtable.vote.dto.request.CreateVoteRequest;
import org.sopt.catchtable.vote.dto.response.CreateVoteResponse;
import org.sopt.catchtable.vote.repository.VoteCandidateRepository;
import org.sopt.catchtable.vote.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 투표 생성이 실제 DB에 어떻게 저장되는지 검증하는 통합 테스트.
 *
 * @Transactional을 붙이면 VoteService에 @Transactional이 빠져도 테스트 트랜잭션에 참여해 통과하므로 붙이지 않는다.
 * 대신 이 테스트가 만든 데이터만 골라서 지우고(외래키 때문에 자식 테이블부터), 검증도 그 데이터만 본다.
 * 그래서 연결한 DB에 원래 있던 데이터는 건드리지 않는다.
 */
@SpringBootTest
class VoteServiceIntegrationTest {

    // DB에 존재할 수 없는 ID (자동 증가 ID가 여기까지 도달하지 않음)
    private static final Long NON_EXISTENT_ID = Long.MAX_VALUE;

    @Autowired
    private VoteService voteService;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteCandidateRepository voteCandidateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdStoreIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        List<VoteCandidate> candidates = createdCandidates();
        List<Vote> votes = createdVotes();

        voteCandidateRepository.deleteAllByIdInBatch(candidates.stream().map(VoteCandidate::getId).toList());
        voteRepository.deleteAllByIdInBatch(votes.stream().map(Vote::getId).toList());
        storeRepository.deleteAllByIdInBatch(createdStoreIds);
        userRepository.deleteAllByIdInBatch(createdUserIds);
    }

    @DisplayName("투표를 만들면 진행 중 투표 1건과, 요청한 순서대로 정렬된 후보 2개가 저장된다")
    @Test
    void createVote() {
        // given
        User hostUser = saveUser("생성자");
        Store first = saveStore("첫 번째 매장");
        Store second = saveStore("두 번째 매장");

        CreateVoteRequest request = new CreateVoteRequest(List.of(second.getId(), first.getId()));

        // when
        CreateVoteResponse response = voteService.createVote(hostUser.getId(), request);

        // then
        assertThat(createdVotes()).hasSize(1)
            .extracting(Vote::getId, Vote::getStatus)
            .containsExactly(tuple(response.voteId(), VoteStatus.OPEN));

        assertThat(createdCandidates())
            .extracting(
                candidate -> candidate.getVote().getId(),
                candidate -> candidate.getStore().getId(),
                candidate -> candidate.getDisplayOrder()
            )
            .containsExactlyInAnyOrder(
                tuple(response.voteId(), second.getId(), 1),
                tuple(response.voteId(), first.getId(), 2)
            );
    }

    @DisplayName("존재하지 않는 매장이 후보에 있으면 투표와 후보 모두 저장되지 않는다")
    @Test
    void createVoteWithUnknownStore() {
        // given
        User hostUser = saveUser("생성자");
        Store store = saveStore("있는 매장");

        CreateVoteRequest request = new CreateVoteRequest(List.of(store.getId(), NON_EXISTENT_ID));

        // when & then
        assertThatThrownBy(() -> voteService.createVote(hostUser.getId(), request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORE_NOT_FOUND);

        assertThat(createdVotes()).isEmpty();
        assertThat(createdCandidates()).isEmpty();
    }

    @DisplayName("등록되지 않은 사용자의 요청으로는 투표가 저장되지 않는다")
    @Test
    void createVoteByUnknownUser() {
        // given
        Store first = saveStore("첫 번째 매장");
        Store second = saveStore("두 번째 매장");

        CreateVoteRequest request = new CreateVoteRequest(List.of(first.getId(), second.getId()));

        // when & then
        assertThatThrownBy(() -> voteService.createVote(NON_EXISTENT_ID, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThat(createdCandidates()).isEmpty();
    }

    private User saveUser(String nickname) {
        User user = userRepository.save(EntityFixture.newUser(nickname));
        createdUserIds.add(user.getId());
        return user;
    }

    private Store saveStore(String name) {
        Store store = storeRepository.save(EntityFixture.newStore(name));
        createdStoreIds.add(store.getId());
        return store;
    }

    // 이 테스트가 만든 사용자가 생성한 투표
    private List<Vote> createdVotes() {
        return voteRepository.findAll().stream()
            .filter(vote -> createdUserIds.contains(vote.getHostUser().getId()))
            .toList();
    }

    // 이 테스트가 만든 매장을 가리키는 후보 (투표 생성자와 관계없이 모두 포함)
    private List<VoteCandidate> createdCandidates() {
        return voteCandidateRepository.findAll().stream()
            .filter(candidate -> createdStoreIds.contains(candidate.getStore().getId()))
            .toList();
    }
}
