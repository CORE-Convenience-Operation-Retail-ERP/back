package com.core.erp.repository;

import com.core.erp.domain.DisposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DisposalRepository extends JpaRepository<DisposalEntity, Integer> {

    List<DisposalEntity> findAllByOrderByDisposalDateDesc();

    List<DisposalEntity> findByProduct_ProNameContainingAndDisposalDateBetweenOrderByDisposalDateDesc(
            String keyword, LocalDateTime start, LocalDateTime end
    );

    List<DisposalEntity> findByStoreStock_Store_StoreIdOrderByDisposalDateDesc(Integer storeId);

    List<DisposalEntity> findByStoreStock_Store_StoreIdAndProduct_ProNameContainingAndDisposalDateBetweenOrderByDisposalDateDesc(
            Integer storeId, String keyword, LocalDateTime start, LocalDateTime end
    );
}
