package org.sopt.catchtable.store.service;

import java.util.Collection;
import java.util.Map;
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
    public Map<Long, Store> getStoresByIds(Collection<Long> storeIds) {
        Map<Long, Store> stores = storeRepository.findAllByIdIn(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity()));

        if (stores.size() != storeIds.size()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }

        return stores;
    }
}
