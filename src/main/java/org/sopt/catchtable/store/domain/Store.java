package org.sopt.catchtable.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 20)
    private String region;

    @Column(nullable = false, length = 100)
    private String intro;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(
            name = "review_count",
            nullable = false,
            columnDefinition = "INT DEFAULT 0"
    )
    private Integer reviewCount = 0;

    @Column(name = "station_name", nullable = false, length = 20)
    private String stationName;

    @Column(name = "station_lines", nullable = false, length = 50)
    private String stationLines;

    @Column(name = "station_distance_m", nullable = false)
    private Integer stationDistanceM;

    @Column(name = "price_range", nullable = false, length = 50)
    private String priceRange;

    @Column(name = "business_hours", nullable = false, length = 100)
    private String businessHours;

    @Column(length = 20)
    private String phone;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;
}