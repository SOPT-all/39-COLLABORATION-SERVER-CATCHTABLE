package org.sopt.catchtable.store.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

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
}
