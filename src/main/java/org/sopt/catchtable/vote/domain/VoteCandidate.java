package org.sopt.catchtable.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.catchtable.global.domain.BaseEntity;
import org.sopt.catchtable.store.domain.Store;

@Entity
@Table(
        name = "vote_candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vote_candidates_vote_store",
                        columnNames = {"vote_id", "store_id"}
                ),
                @UniqueConstraint(
                        name = "uk_vote_candidates_vote_order",
                        columnNames = {"vote_id", "display_order"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteCandidate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false, updatable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(
            name = "display_order",
            nullable = false,
            updatable = false
    )
    private Integer displayOrder;
}