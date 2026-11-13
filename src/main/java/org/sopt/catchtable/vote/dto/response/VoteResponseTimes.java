package org.sopt.catchtable.vote.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

// 투표 응답의 시각(createdAt, closedAt)을 명세 형식(2026-11-06T19:30:00+09:00)으로 바꾼다
final class VoteResponseTimes {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private VoteResponseTimes() {
    }

    // @CreationTimestamp의 LocalDateTime은 JVM 기본 시간대 기준이므로,
    // 그 시간대로 해석한 뒤 서울 시각으로 바꾼다 (JVM이 UTC여도 응답은 +09:00 기준으로 맞음)
    // 명세 형식에 맞춰 초 단위로 자르고, 값이 없으면(진행 중 투표의 closedAt) null을 그대로 돌려준다
    static OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.truncatedTo(ChronoUnit.SECONDS)
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(SEOUL)
            .toOffsetDateTime();
    }
}
