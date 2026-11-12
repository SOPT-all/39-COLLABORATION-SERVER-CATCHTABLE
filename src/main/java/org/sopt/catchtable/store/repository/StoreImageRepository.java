package org.sopt.catchtable.store.repository;

import org.sopt.catchtable.store.domain.StoreImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {
    List<StoreImage> findAllByStoreIdInOrderByStoreIdAscDisplayOrderAsc(
            Collection<Long> storeIds
    );
}
