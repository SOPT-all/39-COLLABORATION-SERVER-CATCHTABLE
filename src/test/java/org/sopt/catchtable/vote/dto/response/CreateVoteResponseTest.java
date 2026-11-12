package org.sopt.catchtable.vote.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.user.domain.User;
import org.sopt.catchtable.vote.domain.Vote;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class CreateVoteResponseTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void saveTimeZone() {
        originalTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void JVM_시간대가_UTC여도_createdAt은_서울_시각으로_변환된다() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        CreateVoteResponse response = CreateVoteResponse.of(
            vote(LocalDateTime.of(2026, 11, 6, 10, 30, 0, 500_850_000)),
            List.of()
        );

        // UTC 10:30 = KST 19:30, 소수점 초는 버림
        assertThat(response.createdAt()).hasToString("2026-11-06T19:30+09:00");
    }

    @Test
    void JVM_시간대가_서울이면_createdAt을_그대로_쓴다() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        CreateVoteResponse response = CreateVoteResponse.of(
            vote(LocalDateTime.of(2026, 11, 6, 19, 30)),
            List.of()
        );

        assertThat(response.createdAt()).hasToString("2026-11-06T19:30+09:00");
    }

    private static Vote vote(LocalDateTime createdAt) {
        User host = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(host, "id", 1L);
        Vote vote = Vote.create(host);
        ReflectionTestUtils.setField(vote, "id", 10L);
        ReflectionTestUtils.setField(vote, "createdAt", createdAt);
        return vote;
    }
}
