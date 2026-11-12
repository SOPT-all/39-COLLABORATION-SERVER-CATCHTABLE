package org.sopt.catchtable.vote.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.fixture.EntityFixture;
import org.sopt.catchtable.user.domain.User;

class VoteTest {

    @DisplayName("새로 만든 투표는 진행 중 상태로 시작하고, 만든 사람이 생성자가 된다")
    @Test
    void create() {
        // given
        User hostUser = EntityFixture.userWithId(1L);

        // when
        Vote vote = Vote.create(hostUser);

        // then
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.OPEN);
        assertThat(vote.getClosedAt()).isNull();
        assertThat(vote.getHostUser()).isSameAs(hostUser);
    }
}
