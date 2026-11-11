package org.sopt.catchtable.store.repository;

import org.sopt.catchtable.store.domain.Store;
import org.sopt.catchtable.store.domain.StoreMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreMenuRepository extends JpaRepository<StoreMenu, Long> {
    List<StoreMenu> findAllByStore_IdOrderByDisplayOrderAsc(Long storeId);
}