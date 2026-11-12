package org.sopt.catchtable.store.service;

import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.*;
import org.sopt.catchtable.store.dto.StoreDetailResponse;
import org.sopt.catchtable.store.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreAiSummaryRepository storeAiSummaryRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreAmenityRepository storeAmenityRepository;

    @Transactional(readOnly = true)
    public StoreDetailResponse getStoreDetail(Long storeId) {
        // 매장 조회 및 존재 여부 확인
        Optional<Store> storeOptional = storeRepository.findById(storeId);
        Store store = storeOptional.orElseThrow(
                () -> new BusinessException(ErrorCode.STORE_NOT_FOUND)
        );

        // 매장 부가 정보 조회
        List<StoreImage> storeImages = storeImageRepository.findAllByStore_IdOrderByDisplayOrderAsc(storeId);
        List<StoreMenu> menus = storeMenuRepository.findAllByStore_IdOrderByDisplayOrderAsc(storeId);
        StoreAiSummary storeAiSummary = storeAiSummaryRepository.findByStore_Id(storeId);
        List<StoreAmenity> storeAmenities = storeAmenityRepository.findAllByStore_Id(storeId);

        // 역 정보 구성 및 노선 문자열 분리
        List<String> lines = Arrays.stream(store.getStationLines().split("/"))
                .map(String::trim)
                .toList();

        StoreDetailResponse.StationResponse station =
                StoreDetailResponse.StationResponse.builder()
                        .name(store.getStationName())
                        .lines(lines)
                        .distanceMeters(store.getStationDistanceM())
                        .build();

        // 노출 순서대로 매장 이미지 URL 추출
        List<String> imageUrls = storeImages.stream()
                .map(storeImage -> storeImage.getImageUrl())
                .toList();

        // 대표 메뉴 목록을 응답 DTO로 변환
        List<StoreDetailResponse.RepresentativeMenuResponse> representativeMenus =
                        menus.stream()
                        .map(menu -> StoreDetailResponse.RepresentativeMenuResponse.builder()
                                .menuId(menu.getId())
                                .name(menu.getName())
                                .price(menu.getPrice())
                                .isPriceFrom(menu.isPriceFrom())
                                .imageUrl(menu.getImageUrl())
                                .displayOrder(menu.getDisplayOrder())
                                .build())
                        .toList();

        // AI 요약이 있는 경우에만 응답 DTO 구성
        StoreDetailResponse.AiSummaryResponse aiSummary = null;

        if (storeAiSummary != null) {
            List<String> summaryLines = Stream.of(
                            storeAiSummary.getLine1(),
                            storeAiSummary.getLine2(),
                            storeAiSummary.getLine3()
                    )
                    .filter(Objects::nonNull)
                    .toList();

            aiSummary = StoreDetailResponse.AiSummaryResponse.builder()
                    .title(storeAiSummary.getTitle())
                    .lines(summaryLines)
                    .build();
        }

        // 편의 정보 목록을 응답 DTO로 변환
        List<StoreDetailResponse.AmenityResponse> amenities =
                storeAmenities.stream()
                        .map(amenity -> StoreDetailResponse.AmenityResponse.builder()
                                .category(amenity.getType().name())
                                .content(amenity.getContent())
                                .build())
                        .toList();

        // 매장 상세 응답 조립 및 반환
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
                .amenities(amenities)
                .build();
    }
}
