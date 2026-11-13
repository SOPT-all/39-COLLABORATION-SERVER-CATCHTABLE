package org.sopt.catchtable.vote.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class MyActiveVoteResponse {

    private VoteResponse vote;

    @Getter
    @Builder
    public static class VoteResponse {
        private Long voteId;
        private Integer candidateCount;
        private OffsetDateTime createdAt;
    }
}
