package org.sopt.catchtable.store.repository;

import org.sopt.catchtable.store.domain.StoreAiSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreAiSummaryRepository extends JpaRepository<StoreAiSummary, Long> {
    StoreAiSummary findByStore_Id(Long storeId);
}