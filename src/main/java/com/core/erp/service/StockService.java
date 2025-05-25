package com.core.erp.service;

import com.core.erp.domain.*;
import com.core.erp.dto.*;
import com.core.erp.dto.order.PurchaseOrderItemDTO;
import com.core.erp.dto.stock.*;
import com.core.erp.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class StockService {

    private final ProductRepository productRepository;
    private final StockInHistoryRepository stockInHistoryRepository;
    private final StoreStockRepository storeStockRepository;
    private final StockAdjustLogRepository stockAdjustLogRepository;
    private final PartTimerRepository partTimerRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final StockInventoryCheckItemRepository stockInventoryCheckItemRepository;
    private final WarehouseStockRepository warehouseStockRepository;


    public Page<StockInHistoryDTO> getStockInHistory(Integer storeId, String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inDate").descending());
        Page<StockInHistoryEntity> historyPage;

        if ("ROLE_MASTER".equals(role)) {
            historyPage = stockInHistoryRepository.findAll(pageable);
        } else {
            historyPage = stockInHistoryRepository.findByStore_StoreId(storeId, pageable);
        }

        List<StockInHistoryDTO> dtoList = historyPage.getContent().stream()
                .map(StockInHistoryDTO::new)
                .toList();

        return new PageImpl<>(dtoList, pageable, historyPage.getTotalElements());
    }

    public Page<StockInHistoryDTO> filterStockInHistory(
            Integer storeId,
            String role,
            LocalDateTime from,
            LocalDateTime to,
            Integer status,
            Boolean isAbnormal,
            String productName,
            String barcode,
            String partTimerName,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inDate").descending());

        // HQ는 전체 조회, 점주는 자신의 매장만
        Integer searchStoreId = "ROLE_MASTER".equals(role) ? null : storeId;

        Page<StockInHistoryEntity> historyPage = stockInHistoryRepository.filterHistory(
                searchStoreId,
                status,
                from,
                to,
                isAbnormal,
                productName,
                barcode,
                partTimerName,
                pageable
        );

        List<StockInHistoryDTO> dtoList = historyPage.getContent().stream()
                .map(StockInHistoryDTO::new)
                .toList();

        return new PageImpl<>(dtoList, pageable, historyPage.getTotalElements());
    }
    /**
     * 재고 수동 조정 및 로그 저장
     */
    @Transactional
    public void adjustStock(StockAdjustDTO dto, CustomPrincipal user) {
        PartTimerEntity partTimer = partTimerRepository.findById(dto.getPartTimerId())
                .orElseThrow(() -> new IllegalArgumentException("해당 아르바이트가 존재하지 않습니다."));

        if (!"ROLE_HQ".equals(user.getRole()) &&
                !user.getStoreId().equals(partTimer.getStore().getStoreId())) {
            throw new SecurityException("해당 아르바이트는 귀하의 매장 소속이 아닙니다.");
        }

        StoreStockEntity stock = storeStockRepository
                .findByStore_StoreIdAndProduct_ProductId(dto.getStoreId(), dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("재고 정보가 없습니다."));

        int prev = stock.getQuantity();
        stock.setQuantity(dto.getNewQuantity());
        storeStockRepository.save(stock);

        StockAdjustLogEntity log = dto.toEntity(stock.getStore(), stock.getProduct(), partTimer.getPartName(), prev);
        stockAdjustLogRepository.save(log);
    }

    /**
     * 전체 조정 로그 조회 (권한별)
     */
    public Page<StockAdjustLogDTO> getAdjustmentLogs(Integer storeId, String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("adjustDate").descending());

        Page<StockAdjustLogEntity> logs = "ROLE_HQ".equals(role)
                ? stockAdjustLogRepository.findAll(pageable)
                : stockAdjustLogRepository.findByStore_StoreId(storeId, pageable);

        List<StockAdjustLogDTO> dtoList = logs.stream()
                .map(StockAdjustLogDTO::new)
                .toList();

        return new PageImpl<>(dtoList, pageable, logs.getTotalElements());
    }

    /**
     * 필터 검색 로그 (기간, 이름, 상품명)
     */

    public Page<StockAdjustLogDTO> filterAdjustmentLogs(
            Integer storeId,
            String role,
            LocalDateTime from,
            LocalDateTime to,
            String adjustedBy,
            String productName,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("adjustDate").descending());

        Integer searchStoreId = "ROLE_MASTER".equals(role) ? null : storeId;

        Page<StockAdjustLogEntity> filtered = stockAdjustLogRepository.filterLogs(
                searchStoreId, from, to, adjustedBy, productName, pageable);

        List<StockAdjustLogDTO> dtoList = filtered.stream()
                .map(StockAdjustLogDTO::new)
                .toList();

        return new PageImpl<>(dtoList, pageable, filtered.getTotalElements());
    }

    public List<PurchaseOrderItemDTO> getPendingStockItems(Integer storeId) {
        List<PurchaseOrderItemEntity> items = purchaseOrderItemRepository.findPendingItemsByStore(storeId);
        return items.stream().map(PurchaseOrderItemDTO::new).toList();
    }


    public Page<TotalStockDTO> getStockSummary(Integer productId, Integer storeId, String productName, Long barcode, Integer categoryId, Pageable pageable
    ) {
        return storeStockRepository.findStockSummary(productId, storeId, productName, barcode, categoryId, pageable);
    }

    public StockDetailDTO findStockDetail(Long productId, int storeId) {

        // [1] 상품 정보 조회
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // [2] 매장 재고 조회
        StoreStockEntity storeStock = storeStockRepository
                .findByStore_StoreIdAndProduct_ProductId(storeId, Math.toIntExact(productId))
                .orElse(null);

        // [3] 창고 재고 조회
        WarehouseStockEntity warehouseStock = warehouseStockRepository
                .findByStore_StoreIdAndProduct_ProductId(storeId, Math.toIntExact(productId))
                .orElse(null);

        // [4] 최신 실사 정보 조회
        StockInventoryCheckItemEntity checkItem = stockInventoryCheckItemRepository
                .findTopByStoreIdAndProductIdOrderByCheckDateDesc(storeId, Math.toIntExact(productId))
                .orElse(null);

        // [5] DTO 생성 및 반환
        return new StockDetailDTO(
                product.getProName(),
                product.getProBarcode(),
                product.getIsPromo(),
                storeStock != null ? storeStock.getQuantity() : 0,
                warehouseStock != null ? warehouseStock.getQuantity() : 0,
                checkItem != null ? checkItem.getStoreRealQuantity() : 0,
                checkItem != null ? checkItem.getWarehouseRealQuantity() : 0,
                storeStock != null ? storeStock.getLocationCode() : null
        );
    }


}
