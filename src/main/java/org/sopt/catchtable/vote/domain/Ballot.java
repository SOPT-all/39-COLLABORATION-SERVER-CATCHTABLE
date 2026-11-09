package org.sopt.catchtable.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.catchtable.user.domain.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ballots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ballots_vote_user",
                        columnNames = {"vote_id", "user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_ballots_vote_anonymous_no",
                        columnNames = {"vote_id", "anonymous_no"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ballot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "name_type",
            nullable = false,
            columnDefinition = "VARCHAR(10)"
    )
    private NameType nameType;

    @Column(name = "display_name", nullable = false, length = 20)
    private String displayName;

    @Column(name = "anonymous_no")
    private Integer anonymousNo;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;
}