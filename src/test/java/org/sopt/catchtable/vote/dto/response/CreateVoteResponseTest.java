package org.sopt.catchtable.vote.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.fixture.EntityFixture;
import org.sopt.catchtable.vote.domain.Vote;

class CreateVoteResponseTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
    }

    // 다른 테스트에 영향을 주지 않도록 JVM 시간대를 되돌린다
    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @DisplayName("DB에 저장된 KST 시각은 서버 시간대가 UTC여도 그대로 서울 시각(+09:00)으로, 초 단위까지만 응답한다")
    @Test
    void createdAtOnUtcServer() {
        // given
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        LocalDateTime kstCreatedAt = LocalDateTime.of(2026, 11, 6, 19, 30, 0, 500_850_000);
        Vote vote = EntityFixture.savedVote(10L, EntityFixture.userWithId(1L), kstCreatedAt);

        // when
        CreateVoteResponse response = CreateVoteResponse.of(vote, List.of());

        // then
        assertThat(response.createdAt()).hasToString("2026-11-06T19:30+09:00");
    }

    @DisplayName("서버 시간대가 서울이어도 같은 KST 시각에는 같은 응답을 준다")
    @Test
    void createdAtOnSeoulServer() {
        // given
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        LocalDateTime kstCreatedAt = LocalDateTime.of(2026, 11, 6, 19, 30, 0, 500_850_000);
        Vote vote = EntityFixture.savedVote(10L, EntityFixture.userWithId(1L), kstCreatedAt);

        // when
        CreateVoteResponse response = CreateVoteResponse.of(vote, List.of());

        // then
        assertThat(response.createdAt()).hasToString("2026-11-06T19:30+09:00");
    }
}
