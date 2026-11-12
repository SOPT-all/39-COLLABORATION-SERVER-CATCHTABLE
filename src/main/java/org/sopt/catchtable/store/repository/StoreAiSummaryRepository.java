package org.sopt.catchtable.store.repository;

import org.sopt.catchtable.store.domain.StoreAiSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreAiSummaryRepository extends JpaRepository<StoreAiSummary, Long> {
    List<StoreAiSummary> findAllByStoreIdIn(Collection<Long> storeIds);
}