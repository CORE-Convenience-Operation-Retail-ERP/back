package com.core.erp.repository;

import com.core.erp.domain.SalesSettleEntity;
import com.core.erp.domain.SalesSettleEntity.SettlementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesSettleRepository extends JpaRepository<SalesSettleEntity, Integer> {

    // 날짜 + 타입 목록 조회 (기간 조건이 있을 때 사용)

    List<SalesSettleEntity> findByStore_StoreIdAndSettlementDateBetweenAndSettlementTypeIn(
            Integer storeId,
            LocalDate startDate,
            LocalDate endDate,
            List<SettlementType> types
    );

    // 타입 목록 조회 (기간 조건이 없을 때 사용)
    List<SalesSettleEntity> findByStore_StoreIdAndSettlementTypeIn(
            Integer storeId,
            List<SettlementType> types
    );

    // 기간 내, 여러 SettlementType 조회 + 날짜 오름차순 정렬
    List<SalesSettleEntity> findByStore_StoreIdAndSettlementDateBetweenAndSettlementTypeInOrderBySettlementDateAsc(
            Integer storeId,
            LocalDate startDate,
            LocalDate endDate,
            List<SettlementType> types
    );

    // 전체 기간, 여러 SettlementType 조회 + 날짜 오름차순 정렬
    List<SalesSettleEntity> findByStore_StoreIdAndSettlementTypeInOrderBySettlementDateAsc(
            Integer storeId,
            List<SettlementType> types
    );
}

