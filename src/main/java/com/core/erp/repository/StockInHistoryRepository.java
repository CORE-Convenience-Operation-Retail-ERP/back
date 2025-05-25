package com.core.erp.repository;

import com.core.erp.domain.StockInHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockInHistoryRepository extends JpaRepository<StockInHistoryEntity, Long> {
    List<StockInHistoryEntity> findTop3ByProduct_ProductIdOrderByInDateDesc(int productId);
    StockInHistoryEntity findTop1ByProduct_ProductIdOrderByInDateDesc(int productId);
    List<StockInHistoryEntity> findByProduct_ProductIdOrderByInDateDesc(int productId);

    /**
     * 점주 기본 조회 (본인 매장 입고 이력)
     */
    Page<StockInHistoryEntity> findByStore_StoreId(Integer storeId, Pageable pageable);

    /**
     * 필터 검색 (HQ 또는 점주 모두 대응)
     */
    @Query("""
    SELECT s FROM StockInHistoryEntity s
    WHERE (:storeId IS NULL OR s.store.storeId = :storeId)
      AND (:status IS NULL OR s.historyStatus = :status)
      AND (:from IS NULL OR s.inDate >= :from)
      AND (:to IS NULL OR s.inDate <= :to)
      AND (:isAbnormal IS NULL OR (:isAbnormal = true AND s.historyStatus = 4))
      AND (:productName IS NULL OR s.product.proName LIKE %:productName%)
      AND (:barcode IS NULL OR CAST(s.product.proBarcode AS string) LIKE %:barcode%)
      AND (:partTimerName IS NULL OR s.partTimer.partName LIKE %:partTimerName%)
""")

    Page<StockInHistoryEntity> filterHistory(
            @Param("storeId") Integer storeId,
            @Param("status") Integer status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("isAbnormal") Boolean isAbnormal,
            @Param("productName") String productName,
            @Param("barcode") String barcode,
            @Param("partTimerName") String partTimerName,
            Pageable pageable
    );

    // KPI: 오늘 날짜 기준, 특정 매장의 입고 수량 합계
    @Query("""
SELECT COALESCE(SUM(s.inQuantity), 0)
FROM StockInHistoryEntity s
WHERE s.store.storeId = :storeId
  AND s.inDate BETWEEN :start AND :end
""")
    int sumStockInQuantityByStoreAndPeriod(
            @Param("storeId") Integer storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


}