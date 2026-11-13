package org.sopt.catchtable.store.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.domain.StoreAiSummary;
import org.sopt.catchtable.store.domain.StoreAmenity;
import org.sopt.catchtable.store.domain.StoreImage;
import org.sopt.catchtable.store.domain.StoreMenu;
import org.sopt.catchtable.store.dto.response.StoreDetailResponse;
import org.sopt.catchtable.store.repository.StoreAiSummaryRepository;
import org.sopt.catchtable.store.repository.StoreAmenityRepository;
import org.sopt.catchtable.store.repository.StoreImageRepository;
import org.sopt.catchtable.store.repository.StoreMenuRepository;
import org.sopt.catchtable.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreAiSummaryRepository storeAiSummaryRepository;
    private final StoreAmenityRepository storeAmenityRepository;

    // 여러 매장을 한 번에 조회, 하나라도 없으면 STORE_NOT_FOUND
    // findAllByIdIn은 결과 순서를 보장하지 않으므로 ID로 찾을 수 있게 Map으로 반환
    // 같은 ID가 여러 번 들어와도 호출하는 쪽에 기대지 않도록 중복을 없앤 뒤 비교한다
    public Map<Long, Store> getStoresByIds(Collection<Long> storeIds) {
        Set<Long> uniqueStoreIds = new HashSet<>(storeIds);

        Map<Long, Store> stores = storeRepository.findAllByIdIn(uniqueStoreIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity()));

        if (stores.size() != uniqueStoreIds.size()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }

        return stores;
    }

    // 단건 조회: 공통 조회를 재사용하고 매장 존재 여부 확인
    public StoreDetailResponse getStoreDetail(Long storeId) {
        StoreDetailResponse response =
                getStoreDetails(List.of(storeId)).get(storeId);

        if (response == null) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }

        return response;
    }

    // 일괄 조회: 매장 ID별 상세 응답 반환
    public Map<Long, StoreDetailResponse> getStoreDetails(
            Collection<Long> storeIds
    ) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }

        // 매장 기본 정보 조회
        List<Store> stores = storeRepository.findAllByIdIn(storeIds);

        // 이미지·메뉴를 노출 순서대로 조회하고 매장별로 분류
        Map<Long, List<StoreImage>> imagesByStoreId =
                storeImageRepository
                        .findAllByStoreIdInOrderByStoreIdAscDisplayOrderAsc(storeIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                image -> image.getStore().getId()
                        ));

        Map<Long, List<StoreMenu>> menusByStoreId =
                storeMenuRepository
                        .findAllByStoreIdInOrderByStoreIdAscDisplayOrderAsc(storeIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                menu -> menu.getStore().getId()
                        ));

        // 매장별 AI 요약 조회
        Map<Long, StoreAiSummary> summariesByStoreId =
                storeAiSummaryRepository.findAllByStoreIdIn(storeIds)
                        .stream()
                        .collect(Collectors.toMap(
                                StoreAiSummary::getStoreId,
                                Function.identity()
                        ));

        // 편의 정보를 조회하고 매장별로 분류
        Map<Long, List<StoreAmenity>> amenitiesByStoreId =
                storeAmenityRepository
                        .findAllByStoreIdInOrderByStoreIdAscTypeAsc(storeIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                amenity -> amenity.getStore().getId()
                        ));

        // 공통 변환 메서드로 매장별 상세 응답 구성
        Map<Long, StoreDetailResponse> responses = new HashMap<>();

        for (Store store : stores) {
            Long storeId = store.getId();

            responses.put(
                    storeId,
                    StoreDetailResponse.from(
                            store,
                            imagesByStoreId.getOrDefault(storeId, List.of()),
                            menusByStoreId.getOrDefault(storeId, List.of()),
                            summariesByStoreId.get(storeId),
                            amenitiesByStoreId.getOrDefault(storeId, List.of())
                    )
            );
        }

        return responses;
    }
}
