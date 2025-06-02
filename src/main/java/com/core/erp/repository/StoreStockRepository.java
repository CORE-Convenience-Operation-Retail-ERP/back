package com.core.erp.repository;

import com.core.erp.domain.StoreStockEntity;
import com.core.erp.dto.disposal.DisposalTargetProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface StoreStockRepository extends JpaRepository<StoreStockEntity, Long>, StockRepositoryCustom {
    @Query("SELECT SUM(s.quantity) FROM StoreStockEntity s WHERE s.product.productId = :productId")
    Integer sumStockByProductId(Long productId);


    //  상품 ID로 매장 재고 목록 조회
    @Query("""
SELECT s FROM StoreStockEntity s
WHERE s.product.productId = :productId
AND (:storeId IS NULL OR s.store.storeId = :storeId)
""")
    List<StoreStockEntity> findByProduct_ProductId(
            @Param("productId") int productId,
            @Param("storeId") Integer storeId
    );

    //  상품 ID로 매장 재고 목록 조회
    List<StoreStockEntity> findByProduct_ProductId(int productId);

    //  매장 ID + 상품 ID로 재고 엔티티 조회
    Optional<StoreStockEntity> findByStore_StoreIdAndProduct_ProductId(
            Integer storeId,
            int productId
    );

    //  매장 ID + 상품 ID로 수량만 조회
    @Query("""
        SELECT s.quantity 
        FROM StoreStockEntity s 
        WHERE s.product.productId = :productId 
          AND s.store.storeId = :storeId
    """)
    Optional<Integer> findQuantityByProductAndStore(
            @Param("productId") int productId,
            @Param("storeId") Integer storeId
    );

    // 전체 매장 유통기한 지난 재고 조회
    @Query(value = """
        SELECT 
            s.stock_id AS stockId,
            p.product_id AS productId,
            p.pro_name AS proName,
            s.quantity AS quantity,
            s.last_in_date AS lastInDate,
            DATE_ADD(s.last_in_date, INTERVAL p.expiration_period DAY) AS expiredDate
        FROM store_stock s
        JOIN product p ON s.product_id = p.product_id
        WHERE DATE_ADD(s.last_in_date, INTERVAL p.expiration_period DAY) < CURRENT_TIMESTAMP
          AND s.quantity > 0
    """, nativeQuery = true)
    List<DisposalTargetProjection> findExpiredDisposals();

    // 매장별 유통기한 지난 재고 조회
    @Query(value = """
        SELECT 
            s.stock_id AS stockId,
            p.product_id AS productId,
            p.pro_name AS proName,
            s.quantity AS quantity,
            s.last_in_date AS lastInDate,
            DATE_ADD(s.last_in_date, INTERVAL p.expiration_period DAY) AS expiredDate
        FROM store_stock s
        JOIN product p ON s.product_id = p.product_id
        WHERE DATE_ADD(s.last_in_date, INTERVAL p.expiration_period DAY) < CURRENT_TIMESTAMP
          AND s.quantity > 0
          AND s.store_id = :storeId
    """, nativeQuery = true)
    List<DisposalTargetProjection> findExpiredDisposalsByStore(@Param("storeId") Integer storeId);



    Optional<StoreStockEntity> findTopByProduct_ProductIdOrderByLastInDateDesc(Long productId);

    @Transactional
    @Modifying
    @Query("""
UPDATE StoreStockEntity s
SET s.quantity = s.quantity + :qty, s.lastInDate = CURRENT_TIMESTAMP
WHERE s.product.productId = :productId AND s.store.storeId = :storeId
""")
    int increaseQuantityAndUpdateDate(@Param("productId") Long productId,
                                      @Param("storeId") Integer storeId,
                                      @Param("qty") int qty);

    @Transactional
    @Modifying
    @Query("""
UPDATE StoreStockEntity s
SET s.quantity = s.quantity - :qty
WHERE s.product.productId = :productId AND s.store.storeId = :storeId AND s.quantity >= :qty
""")
    int decreaseQuantity(@Param("productId") Long productId,
                         @Param("storeId") Integer storeId,
                         @Param("qty") int qty);

    Optional<StoreStockEntity> findByProduct_ProductIdAndStore_StoreId(Long productId, Integer storeId);

}