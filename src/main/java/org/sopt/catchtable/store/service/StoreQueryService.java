package org.sopt.catchtable.store.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
public class StoreQueryService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreAiSummaryRepository storeAiSummaryRepository;
    private final StoreAmenityRepository storeAmenityRepository;

    public Map<Long, StoreDetailResponse> getStoreDetails(Collection<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }

        List<Store> stores = storeRepository.findAllByIdIn(storeIds);
        Map<Long, List<StoreImage>> imagesByStoreId = storeImageRepository
            .findAllByStoreIdInOrderByStoreIdAscDisplayOrderAsc(storeIds)
            .stream()
            .collect(Collectors.groupingBy(image -> image.getStore().getId()));
        Map<Long, List<StoreMenu>> menusByStoreId = storeMenuRepository
            .findAllByStoreIdInOrderByStoreIdAscDisplayOrderAsc(storeIds)
            .stream()
            .collect(Collectors.groupingBy(menu -> menu.getStore().getId()));
        Map<Long, StoreAiSummary> aiSummaryByStoreId = storeAiSummaryRepository
            .findAllByStoreIdIn(storeIds)
            .stream()
            .collect(Collectors.toMap(StoreAiSummary::getStoreId, Function.identity()));
        Map<Long, List<StoreAmenity>> amenitiesByStoreId = storeAmenityRepository
            .findAllByStoreIdInOrderByStoreIdAscTypeAsc(storeIds)
            .stream()
            .collect(Collectors.groupingBy(amenity -> amenity.getStore().getId()));

        Map<Long, StoreDetailResponse> responses = new HashMap<>();
        for (Store store : stores) {
            Long storeId = store.getId();
            responses.put(storeId, StoreDetailResponse.from(
                store,
                imagesByStoreId.getOrDefault(storeId, List.of()),
                menusByStoreId.getOrDefault(storeId, List.of()),
                aiSummaryByStoreId.get(storeId),
                amenitiesByStoreId.getOrDefault(storeId, List.of())
            ));
        }
        return responses;
    }
}
