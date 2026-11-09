package org.sopt.catchtable.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "store_ai_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreAiSummary {

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String line1;

    @Column(length = 100)
    private String line2;

    @Column(length = 100)
    private String line3;
}