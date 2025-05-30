package com.core.erp.repository;

import com.core.erp.domain.HQStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HQStockRepository extends JpaRepository<HQStockEntity, Integer> {
    
    // 상품 ID로 본사 재고 조회
    Optional<HQStockEntity> findByProductProductId(int productId);
    
    // 본사 재고 수량 업데이트
    @Modifying
    @Query("UPDATE HQStockEntity h SET h.quantity = :quantity, h.updatedBy = :updatedBy WHERE h.product.productId = :productId")
    void updateQuantity(int productId, int quantity, String updatedBy);
    
    // StoreStock의 데이터를 기반으로 본사 재고 업데이트 
    @Modifying
    @Query(value = "UPDATE hq_stock h SET h.quantity = h.total_quantity - COALESCE((SELECT SUM(ss.quantity) FROM store_stock ss WHERE ss.product_id = h.product_id), 0) WHERE h.product_id = :productId", nativeQuery = true)
    void recalculateQuantity(int productId);
    
    // 특정 일자에 정기 입고가 활성화된 모든 재고 조회
    List<HQStockEntity> findAllByRegularInDayAndRegularInActiveTrue(int regularInDay);
}