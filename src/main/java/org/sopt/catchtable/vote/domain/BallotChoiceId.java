package org.sopt.catchtable.vote.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BallotChoiceId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ballot_id")
    private Long ballotId;

    @Column(name = "candidate_id")
    private Long candidateId;

    public BallotChoiceId(Long ballotId, Long candidateId) {
        this.ballotId = ballotId;
        this.candidateId = candidateId;
    }
}