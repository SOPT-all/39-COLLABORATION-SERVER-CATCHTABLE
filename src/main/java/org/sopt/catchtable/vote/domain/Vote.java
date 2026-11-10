package org.sopt.catchtable.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.catchtable.user.domain.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "votes",
        indexes = {
                @Index(
                        name = "idx_votes_host_status_created_at",
                        columnList = "host_user_id, status, created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "host_user_id",
            nullable = false,
            updatable = false
    )
    private User hostUser;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "VARCHAR(10) DEFAULT 'OPEN'"
    )
    private VoteStatus status = VoteStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}