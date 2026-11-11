package org.sopt.catchtable.store.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class StoreDetailResponse {
    private Long storeId;
    private String name;
    private String category;
    private String region;
    private String intro;
    private BigDecimal rating;
    private Integer reviewCount;
    private StationResponse station;
    private String priceRange;
    private String businessHours;
    private String phone;
    private List<String> imageUrls;
    private List<RepresentativeMenuResponse> representativeMenus;
    private AiSummaryResponse aiSummary;
    private List<AmenityResponse> amenities;

    @Getter
    @Builder
    public static class StationResponse {
        private String name;
        private List<String> lines;
        private Integer distanceMeters;
    }

    @Getter
    @Builder
    public static class RepresentativeMenuResponse {
        private Long menuId;
        private String name;
        private Integer price;
        private Boolean isPriceFrom;
        private String imageUrl;
        private Integer displayOrder;
    }

    @Getter
    @Builder
    public static class AiSummaryResponse {
        private String title;
        private List<String> lines;
    }

    @Getter
    @Builder
    public static class AmenityResponse {
        private String category;
        private String content;
    }
}
