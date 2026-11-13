package org.sopt.catchtable.store.repository;

import org.sopt.catchtable.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findAllByIdIn(Collection<Long> storeIds);
}
