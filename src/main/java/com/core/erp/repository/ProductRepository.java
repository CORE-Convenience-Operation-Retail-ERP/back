package com.core.erp.repository;

import com.core.erp.domain.ProductEntity;
import com.core.erp.dto.order.OrderProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 검색 조건에 맞는 제품 수 반환 (재고 포함 안 함)
     */
    @Query(value =
            """
    SELECT COUNT(*)
    FROM product p
LEFT JOIN store_stock s\s
    ON p.product_id = s.product_id AND s.store_id = :storeId
    WHERE p.is_promo IN (0, 2, 3)
      AND (s.store_id = :storeId OR s.store_id IS NULL)
      AND (:productName IS NULL OR p.pro_name LIKE CONCAT('%', :productName, '%'))
      AND (:barcode IS NULL OR p.pro_barcode = :barcode)
      AND (:categoryId IS NULL OR p.category_id = :categoryId)
      AND (:isPromo IS NULL OR p.is_promo = :isPromo)
""", nativeQuery = true)

    int countProductsWithStock(
            @Param("storeId") Integer storeId,
            @Param("productName") String productName,
            @Param("barcode") Long barcode,
            @Param("categoryId") Integer categoryId,
            @Param("isPromo") Integer isPromo
    );


    /* 바코드로 상품 조회 (POS 바코드 기능용) */
    Optional<ProductEntity> findByProBarcode(Long proBarcode);

    @Query("SELECT SUM(s.quantity) FROM StoreStockEntity s WHERE s.product.category.categoryId = :categoryId")
    Integer sumStockByCategory(@Param("categoryId") Integer categoryId);
}
