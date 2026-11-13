package org.sopt.catchtable.vote.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitBallotRequest(
    @NotNull @Positive Long candidateId,
    @NotNull String nickname
) {
}
