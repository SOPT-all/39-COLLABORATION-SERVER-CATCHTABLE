package org.sopt.catchtable.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ballot_choices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BallotChoice {

    @EmbeddedId
    private BallotChoiceId id;

    @MapsId("ballotId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ballot_id", nullable = false)
    private Ballot ballot;

    @MapsId("candidateId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private VoteCandidate candidate;
}