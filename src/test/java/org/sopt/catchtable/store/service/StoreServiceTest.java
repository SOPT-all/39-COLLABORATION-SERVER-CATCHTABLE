package org.sopt.catchtable.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.sopt.catchtable.fixture.EntityFixture.storeWithId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.catchtable.global.exception.BusinessException;
import org.sopt.catchtable.global.exception.ErrorCode;
import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.repository.StoreAiSummaryRepository;
import org.sopt.catchtable.store.repository.StoreAmenityRepository;
import org.sopt.catchtable.store.repository.StoreImageRepository;
import org.sopt.catchtable.store.repository.StoreMenuRepository;
import org.sopt.catchtable.store.repository.StoreRepository;

class StoreServiceTest {

    private StoreRepository storeRepository;
    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeRepository = mock(StoreRepository.class);
        storeService = new StoreService(
            storeRepository,
            mock(StoreImageRepository.class),
            mock(StoreMenuRepository.class),
            mock(StoreAiSummaryRepository.class),
            mock(StoreAmenityRepository.class)
        );
    }

    @DisplayName("요청한 매장을 모두 찾으면, 조회 순서와 관계없이 매장 ID로 각 매장을 찾을 수 있다")
    @Test
    void getStoresByIds() {
        // given
        Store store1 = storeWithId(1L);
        Store store2 = storeWithId(2L);
        given(storeRepository.findAllByIdIn(Set.of(1L, 2L))).willReturn(List.of(store1, store2));

        // when
        Map<Long, Store> stores = storeService.getStoresByIds(List.of(2L, 1L));

        // then
        assertThat(stores)
            .hasSize(2)
            .containsEntry(1L, store1)
            .containsEntry(2L, store2);
    }

    @DisplayName("같은 매장을 여러 번 요청해도, 매장이 존재하면 한 번만 조회해서 찾을 수 있다")
    @Test
    void getStoresByIdsWithDuplicatedIds() {
        // given
        Store store1 = storeWithId(1L);
        given(storeRepository.findAllByIdIn(Set.of(1L))).willReturn(List.of(store1));

        // when
        Map<Long, Store> stores = storeService.getStoresByIds(List.of(1L, 1L));

        // then
        assertThat(stores)
            .hasSize(1)
            .containsEntry(1L, store1);
    }

    @DisplayName("요청한 매장 중 하나라도 존재하지 않으면 매장을 찾을 수 없다")
    @Test
    void getStoresByIdsWithUnknownStore() {
        // given
        given(storeRepository.findAllByIdIn(Set.of(1L, 999L))).willReturn(List.of(storeWithId(1L)));

        // when & then
        assertThatThrownBy(() -> storeService.getStoresByIds(List.of(1L, 999L)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }
}
