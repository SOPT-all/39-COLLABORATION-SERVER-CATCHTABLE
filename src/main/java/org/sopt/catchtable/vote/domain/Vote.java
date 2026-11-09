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
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_votes_host_idempotency_key",
                        columnNames = {"host_user_id", "idempotency_key"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_votes_host_status_created",
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

    @Column(
            name = "anonymous_seq",
            nullable = false,
            columnDefinition = "INT DEFAULT 0"
    )
    private Integer anonymousSeq = 0;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "closed_at", columnDefinition = "DATETIME")
    private LocalDateTime closedAt;
}