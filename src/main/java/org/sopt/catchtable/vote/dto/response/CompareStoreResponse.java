package org.sopt.catchtable.vote.dto.response;

import java.math.BigDecimal;
import java.util.List;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.AiSummaryResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.AmenityResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.RepresentativeMenuResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.StationResponse;

// 후보 매장 비교 정보 (명세의 CompareStore)
public record CompareStoreResponse(
    Long storeId,
    String name,
    BigDecimal rating,
    Integer reviewCount,
    List<String> imageUrls,
    StationResponse station,
    AiSummaryResponse aiSummary,
    String priceRange,
    List<RepresentativeMenuResponse> representativeMenus,
    List<AmenityResponse> amenities
) {

    public static CompareStoreResponse from(StoreDetailResponse store) {
        return new CompareStoreResponse(
            store.getStoreId(),
            store.getName(),
            store.getRating(),
            store.getReviewCount(),
            store.getImageUrls(),
            store.getStation(),
            store.getAiSummary(),
            store.getPriceRange(),
            store.getRepresentativeMenus(),
            store.getAmenities()
        );
    }
}
