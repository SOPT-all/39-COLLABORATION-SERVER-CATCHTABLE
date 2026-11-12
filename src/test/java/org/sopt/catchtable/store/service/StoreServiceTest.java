package org.sopt.catchtable.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.repository.StoreRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class StoreServiceTest {

    private StoreRepository storeRepository;
    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeRepository = mock(StoreRepository.class);
        storeService = new StoreService(storeRepository);
    }

    @Test
    void 요청한_매장을_ID별로_반환한다() {
        Store store1 = store(1L);
        Store store2 = store(2L);
        given(storeRepository.findAllByIdIn(List.of(2L, 1L))).willReturn(List.of(store1, store2));

        Map<Long, Store> stores = storeService.getStoresByIds(List.of(2L, 1L));

        assertThat(stores).containsEntry(1L, store1).containsEntry(2L, store2);
    }

    @Test
    void 없는_매장이_하나라도_있으면_STORE_NOT_FOUND() {
        given(storeRepository.findAllByIdIn(List.of(1L, 999L))).willReturn(List.of(store(1L)));

        assertThatThrownBy(() -> storeService.getStoresByIds(List.of(1L, 999L)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    private static Store store(Long id) {
        Store store = BeanUtils.instantiateClass(Store.class);
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }
}
