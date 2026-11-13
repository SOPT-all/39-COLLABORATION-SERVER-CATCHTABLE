package org.sopt.catchtable.vote.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sopt.catchtable.fixture.EntityFixture.storeWithId;
import static org.sopt.catchtable.fixture.EntityFixture.userWithId;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.user.domain.User;

class VoteTest {

    @DisplayName("새로 만든 투표는 진행 중 상태로 시작하고, 만든 사람이 생성자가 된다")
    @Test
    void create() {
        // given
        User hostUser = userWithId(1L);

        // when
        Vote vote = Vote.create(hostUser);

        // then
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.OPEN);
        assertThat(vote.getClosedAt()).isNull();
        assertThat(vote.getHostUser()).isSameAs(hostUser);
    }

    @DisplayName("서로 다른 매장 2개는 투표 후보가 될 수 있다")
    @Test
    void validateCandidateStoreIds() {
        // given
        List<Long> storeIds = List.of(1L, 2L);

        // when & then
        assertThatCode(() -> Vote.validateCandidateStoreIds(storeIds))
            .doesNotThrowAnyException();
    }

    @DisplayName("투표 후보는 서로 다른 매장 정확히 2개여야 한다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("candidateRuleViolations")
    void validateCandidateStoreIdsWithViolation(String situation, List<Long> storeIds) {
        // when & then
        assertThatThrownBy(() -> Vote.validateCandidateStoreIds(storeIds))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_VOTE_CANDIDATES);
    }

    static Stream<Arguments> candidateRuleViolations() {
        return Stream.of(
            Arguments.of("후보 목록이 없는 경우", null),
            Arguments.of("후보를 하나도 고르지 않은 경우", List.of()),
            Arguments.of("후보를 1개만 고른 경우", List.of(1L)),
            Arguments.of("후보를 3개 고른 경우", List.of(1L, 2L, 3L)),
            Arguments.of("후보에 빈 값이 섞인 경우", Arrays.asList(1L, null)),
            Arguments.of("같은 매장을 두 번 고른 경우", List.of(1L, 1L))
        );
    }

    @DisplayName("투표의 후보를 만들면, 넘겨받은 매장 순서대로 1번·2번 후보가 이 투표에 속한다")
    @Test
    void createCandidates() {
        // given
        Vote vote = Vote.create(userWithId(1L));
        Store first = storeWithId(5L);
        Store second = storeWithId(3L);

        // when
        List<VoteCandidate> candidates = vote.createCandidates(List.of(first, second));

        // then
        assertThat(candidates)
            .extracting(VoteCandidate::getVote, VoteCandidate::getStore, VoteCandidate::getDisplayOrder)
            .containsExactly(
                tuple(vote, first, 1),
                tuple(vote, second, 2)
            );
    }

    @DisplayName("같은 매장으로는 투표의 후보를 만들 수 없다")
    @Test
    void createCandidatesWithSameStore() {
        // given
        Vote vote = Vote.create(userWithId(1L));
        Store store = storeWithId(1L);

        // when & then
        assertThatThrownBy(() -> vote.createCandidates(List.of(store, store)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_VOTE_CANDIDATES);
    }
}
