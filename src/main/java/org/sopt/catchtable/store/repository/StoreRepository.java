package org.sopt.catchtable.store.repository;

import org.sopt.catchtable.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
