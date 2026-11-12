package org.sopt.catchtable.store.repository;

import org.sopt.catchtable.store.domain.StoreAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreAmenityRepository extends JpaRepository<StoreAmenity, Long> {
    List<StoreAmenity> findAllByStoreIdInOrderByStoreIdAscTypeAsc(
            Collection<Long> storeIds
    );
}
