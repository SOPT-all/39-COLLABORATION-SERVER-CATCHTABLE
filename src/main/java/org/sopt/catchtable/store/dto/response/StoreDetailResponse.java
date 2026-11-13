package org.sopt.catchtable.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "가까운 역 정보")
    public static class StationResponse {
        @Schema(description = "역 이름", example = "강남역", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;
        @Schema(description = "호선 목록", example = "[\"2\", \"신분당\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<String> lines;
        @Schema(description = "역까지 거리(m)", example = "350", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer distanceMeters;
    }

    @Getter
    @Builder
    @Schema(description = "대표 메뉴")
    public static class RepresentativeMenuResponse {
        @Schema(description = "메뉴 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long menuId;
        @Schema(description = "메뉴 이름", example = "홍탕 훠궈", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;
        @Schema(description = "가격(원)", example = "32000", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer price;
        @Schema(description = "true면 '32,000원~'처럼 최저가로 표시", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean isPriceFrom;
        @Schema(description = "메뉴 이미지 URL", example = "https://example.com/menu101.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
        private String imageUrl;
        @Schema(description = "노출 순서", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer displayOrder;
    }

    @Getter
    @Builder
    @Schema(description = "AI 요약")
    public static class AiSummaryResponse {
        @Schema(description = "요약 제목", example = "회식하기 좋은 훠궈 맛집", requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;
        @Schema(description = "요약 문장 (최대 3개)", example = "[\"넓은 단체석\", \"친절한 서비스\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
        private List<String> lines;
    }

    @Getter
    @Builder
    @Schema(description = "편의 정보")
    public static class AmenityResponse {
        @Schema(description = "편의 정보 종류. PARKING(주차) / LIQUOR_CORKAGE(주류·콜키지)", example = "PARKING",
            allowableValues = {"PARKING", "LIQUOR_CORKAGE"}, requiredMode = Schema.RequiredMode.REQUIRED)
        private String category;
        @Schema(description = "안내 문구", example = "건물 내 2시간 무료", requiredMode = Schema.RequiredMode.REQUIRED)
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
