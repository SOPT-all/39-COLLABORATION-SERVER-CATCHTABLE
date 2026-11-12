package org.sopt.catchtable.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
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
import org.sopt.catchtable.vote.domain.Vote;
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
 * 테스트에 @Transactional을 붙이지 않는다.
 * 붙이면 VoteService에 @Transactional이 빠져도 테스트 트랜잭션에 참여해서 통과해 버리기 때문이다.
 * 대신 매 테스트 전후에 데이터를 직접 지운다 (외래키 때문에 자식 테이블부터).
 * 실행할 때 연결한 DB의 users, stores, votes, vote_candidates 데이터를 모두 지우므로 테스트 전용 DB에서 실행한다.
 */
@SpringBootTest
class VoteServiceIntegrationTest {

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

    // 다른 테스트나 수동 확인으로 남은 데이터가 결과에 섞이지 않도록 시작 전에도 비운다
    @BeforeEach
    void setUp() {
        tearDown();
    }

    @AfterEach
    void tearDown() {
        voteCandidateRepository.deleteAllInBatch();
        voteRepository.deleteAllInBatch();
        storeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("투표를 만들면 진행 중 투표 1건과, 요청한 순서대로 정렬된 후보 2개가 저장된다")
    @Test
    void createVote() {
        // given
        User hostUser = userRepository.save(EntityFixture.newUser("생성자"));
        Store first = storeRepository.save(EntityFixture.newStore("첫 번째 매장"));
        Store second = storeRepository.save(EntityFixture.newStore("두 번째 매장"));

        CreateVoteRequest request = new CreateVoteRequest(List.of(second.getId(), first.getId()));

        // when
        CreateVoteResponse response = voteService.createVote(hostUser.getId(), request);

        // then
        List<Vote> votes = voteRepository.findAll();
        assertThat(votes).hasSize(1)
            .extracting(Vote::getId, Vote::getStatus)
            .containsExactly(tuple(response.voteId(), VoteStatus.OPEN));

        assertThat(voteCandidateRepository.findAll())
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
        User hostUser = userRepository.save(EntityFixture.newUser("생성자"));
        Store store = storeRepository.save(EntityFixture.newStore("있는 매장"));
        Long unknownStoreId = store.getId() + 1000;

        CreateVoteRequest request = new CreateVoteRequest(List.of(store.getId(), unknownStoreId));

        // when & then
        assertThatThrownBy(() -> voteService.createVote(hostUser.getId(), request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORE_NOT_FOUND);

        assertThat(voteRepository.count()).isZero();
        assertThat(voteCandidateRepository.count()).isZero();
    }

    @DisplayName("등록되지 않은 사용자의 요청으로는 투표가 저장되지 않는다")
    @Test
    void createVoteByUnknownUser() {
        // given
        Store first = storeRepository.save(EntityFixture.newStore("첫 번째 매장"));
        Store second = storeRepository.save(EntityFixture.newStore("두 번째 매장"));
        Long unknownUserId = 999_999L;

        CreateVoteRequest request = new CreateVoteRequest(List.of(first.getId(), second.getId()));

        // when & then
        assertThatThrownBy(() -> voteService.createVote(unknownUserId, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThat(voteRepository.count()).isZero();
    }
}
