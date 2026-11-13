package org.sopt.catchtable.vote.dto.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.AiSummaryResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.AmenityResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.RepresentativeMenuResponse;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse.StationResponse;

// 후보 매장 비교 정보 (명세의 CompareStore)
@Schema(description = "후보 매장 비교 정보 (CompareStore)")
public record CompareStoreResponse(
    @Schema(description = "매장 ID", example = "1", requiredMode = REQUIRED)
    Long storeId,

    @Schema(description = "매장 이름", example = "하이디라오 서초점", requiredMode = REQUIRED)
    String name,

    @Schema(description = "평점 (소수점 1자리)", example = "4.5", requiredMode = REQUIRED)
    BigDecimal rating,

    @Schema(description = "리뷰 수", example = "1234", requiredMode = REQUIRED)
    Integer reviewCount,

    @Schema(description = "매장 이미지 URL. 노출 순서대로, 없으면 []", requiredMode = REQUIRED)
    List<String> imageUrls,

    @Schema(description = "가까운 역 정보", requiredMode = REQUIRED)
    StationResponse station,

    // 객체 참조에 nullable을 붙이면 OpenAPI 3.1에서 타입이 null로만 표시되므로 설명으로 안내
    @Schema(description = "AI 요약. **요약이 없는 매장은 null**", requiredMode = REQUIRED)
    AiSummaryResponse aiSummary,

    @Schema(description = "가격대", example = "3~4만원", requiredMode = REQUIRED)
    String priceRange,

    @Schema(description = "대표 메뉴. 노출 순서대로, 없으면 []", requiredMode = REQUIRED)
    List<RepresentativeMenuResponse> representativeMenus,

    @Schema(description = "편의 정보. 없으면 []", requiredMode = REQUIRED)
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
