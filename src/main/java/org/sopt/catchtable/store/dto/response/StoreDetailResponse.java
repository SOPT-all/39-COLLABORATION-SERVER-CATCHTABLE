package org.sopt.catchtable.store.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.sopt.catchtable.store.domain.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    public static StoreDetailResponse from(
            Store store,
            List<StoreImage> images,
            List<StoreMenu> menus,
            StoreAiSummary summary,
            List<StoreAmenity> amenities
    ) {
        List<String> stationLines =
                Arrays.stream(store.getStationLines().split("/"))
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .toList();

        StationResponse station = StationResponse.builder()
                .name(store.getStationName())
                .lines(stationLines)
                .distanceMeters(store.getStationDistanceM())
                .build();

        List<String> imageUrls = images.stream()
                .map(StoreImage::getImageUrl)
                .toList();

        List<RepresentativeMenuResponse> representativeMenus =
                menus.stream()
                        .map(menu -> RepresentativeMenuResponse.builder()
                                .menuId(menu.getId())
                                .name(menu.getName())
                                .price(menu.getPrice())
                                .isPriceFrom(menu.isPriceFrom())
                                .imageUrl(menu.getImageUrl())
                                .displayOrder(menu.getDisplayOrder())
                                .build())
                        .toList();

        AiSummaryResponse aiSummary = null;

        if (summary != null) {
            List<String> summaryLines = Stream.of(
                            summary.getLine1(),
                            summary.getLine2(),
                            summary.getLine3()
                    )
                    .filter(Objects::nonNull)
                    .toList();

            aiSummary = AiSummaryResponse.builder()
                    .title(summary.getTitle())
                    .lines(summaryLines)
                    .build();
        }

        List<AmenityResponse> amenityResponses = amenities.stream()
                .map(amenity -> AmenityResponse.builder()
                        .category(amenity.getType().name())
                        .content(amenity.getContent())
                        .build())
                .toList();

        return StoreDetailResponse.builder()
                .storeId(store.getId())
                .name(store.getName())
                .category(store.getCategory())
                .region(store.getRegion())
                .intro(store.getIntro())
                .rating(store.getRating())
                .reviewCount(store.getReviewCount())
                .station(station)
                .priceRange(store.getPriceRange())
                .businessHours(store.getBusinessHours())
                .phone(store.getPhone())
                .imageUrls(imageUrls)
                .representativeMenus(representativeMenus)
                .aiSummary(aiSummary)
                .amenities(amenityResponses)
                .build();
    }
}
