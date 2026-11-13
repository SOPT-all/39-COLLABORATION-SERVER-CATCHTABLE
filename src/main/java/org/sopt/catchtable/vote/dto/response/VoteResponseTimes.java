package org.sopt.catchtable.vote.dto.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

// 투표 응답 시각(createdAt, closedAt)을 명세 형식(2026-11-06T19:30:00+09:00)으로 변환
final class VoteResponseTimes {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private VoteResponseTimes() {
    }

    // DB 시각은 KST 기준으로 저장되므로(CatchtableApplication) 서버 시간대와 관계없이 서울 시각으로 해석한다
    // 초 단위로 자르고, null(진행 중 투표의 closedAt)은 그대로 돌려준다
    static OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.truncatedTo(ChronoUnit.SECONDS)
            .atZone(SEOUL)
            .toOffsetDateTime();
    }
}
