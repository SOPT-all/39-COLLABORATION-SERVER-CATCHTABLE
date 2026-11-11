package org.sopt.catchtable.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.catchtable.global.domain.BaseEntity;

@Entity
@Table(
        name = "store_amenities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_store_amenities_store_type",
                        columnNames = {"store_id", "type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreAmenity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private AmenityType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}