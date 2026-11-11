package org.sopt.catchtable.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.catchtable.global.domain.BaseEntity;
import org.sopt.catchtable.user.domain.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ballots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ballots_vote_user",
                        columnNames = {"vote_id", "user_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_ballots_candidate",
                        columnList = "candidate_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ballot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private VoteCandidate candidate;

    @Column(name = "display_name", nullable = false, length = 15)
    private String displayName;
}