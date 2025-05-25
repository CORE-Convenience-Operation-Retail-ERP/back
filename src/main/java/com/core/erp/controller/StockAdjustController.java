package com.core.erp.controller;

import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.stock.StockAdjustDTO;
import com.core.erp.dto.stock.StockAdjustLogDTO;
import com.core.erp.dto.stock.StockInHistoryDTO;
import com.core.erp.service.StockService;
import com.core.erp.service.HQStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockAdjustController {

    private final StockService stockService;
    private final HQStockService hqStockService;

    @PatchMapping("/manual-adjust")
    public ResponseEntity<String> manualAdjustStock(
            @RequestBody StockAdjustDTO dto,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        // 1. 재고 수동 조정 수행
        stockService.adjustStock(dto, user);
        
        // 2. 본사 재고 재계산 (데이터 일관성 유지)
        try {
            hqStockService.recalculateAllHQStocks();
        } catch (Exception e) {
            // 재계산 실패해도 재고 조정은 성공했으므로 로그만 남김
            System.err.println("재고 조정 후 본사 재고 재계산 실패: " + e.getMessage());
        }
        
        return ResponseEntity.ok("재고 수량이 수정되었습니다.");
    }

    @GetMapping("/adjust-log")
    public ResponseEntity<Page<StockAdjustLogDTO>> getAdjustLogs(
            @AuthenticationPrincipal CustomPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StockAdjustLogDTO> result = stockService.getAdjustmentLogs(
                user.getStoreId(), user.getRole(), page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/adjust-log/filter")
    public ResponseEntity<Page<StockAdjustLogDTO>> filterAdjustLogs(
            @AuthenticationPrincipal CustomPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String adjustedBy,
            @RequestParam(required = false) String productName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StockAdjustLogDTO> result = stockService.filterAdjustmentLogs(
                user.getStoreId(), user.getRole(), from, to, adjustedBy, productName, page, size);
        return ResponseEntity.ok(result);
    }
    /** 입고 이력 전체 조회 (권한별) */
    @GetMapping("/adjust/in-history")
    public ResponseEntity<Page<StockInHistoryDTO>> getStockInHistory(
            @AuthenticationPrincipal CustomPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StockInHistoryDTO> result =
                stockService.getStockInHistory(user.getStoreId(), user.getRole(), page, size);
        return ResponseEntity.ok(result);
    }

    /** 입고 이력 필터 조회 (기간·상태·상품명·바코드 등) */
    @GetMapping("/adjust/in-history/filter")
    public ResponseEntity<Page<StockInHistoryDTO>> filterStockInHistory(
            @AuthenticationPrincipal CustomPrincipal user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean isAbnormal,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String partTimerName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StockInHistoryDTO> result = stockService.filterStockInHistory(
                user.getStoreId(), user.getRole(),
                from, to, status, isAbnormal,
                productName, barcode, partTimerName,
                page, size
        );
        return ResponseEntity.ok(result);
    }
}

