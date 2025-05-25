package com.core.erp.controller;

import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.TotalStockDTO;
import com.core.erp.dto.stock.StockDetailDTO;
import com.core.erp.dto.stock.StockInHistoryDTO;
import com.core.erp.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/in-history")
    public ResponseEntity<Page<StockInHistoryDTO>> getStockInHistory(
            @AuthenticationPrincipal CustomPrincipal userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String role = userDetails.getRole();
        Integer storeId = userDetails.getStoreId();

        Page<StockInHistoryDTO> result = stockService.getStockInHistory(storeId, role, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/in-history/filter")
    public ResponseEntity<Page<StockInHistoryDTO>> filterStockInHistory(
            @AuthenticationPrincipal CustomPrincipal userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean isAbnormal,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String partTimerName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String role = userDetails.getRole();
        Integer storeId = userDetails.getStoreId();

        Page<StockInHistoryDTO> result = stockService.filterStockInHistory(
                storeId, role, from, to, status, isAbnormal, productName, barcode, partTimerName, page, size);

        return ResponseEntity.ok(result);
    }
    @GetMapping("/summary")
    public ResponseEntity<Page<TotalStockDTO>> getStockSummary(
            @AuthenticationPrincipal CustomPrincipal userDetails,
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Long barcode,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String role = userDetails.getRole();
        Integer userStoreId = userDetails.getStoreId();
        Integer finalStoreId = role.equals("ROLE_HQ") ? storeId : userStoreId;

        Pageable pageable = PageRequest.of(page, size);

        Page<TotalStockDTO> result = stockService.getStockSummary(
                productId, finalStoreId, productName, barcode, categoryId, pageable
        );

        return ResponseEntity.ok(result);
    }

    //  상품 재고 상세 정보 조회
    @GetMapping("/detail/{productId}")
    public ResponseEntity<StockDetailDTO> getStockDetail(
            @AuthenticationPrincipal CustomPrincipal userDetails,
            @PathVariable Long productId
    ) {
        Integer storeId = userDetails.getStoreId();
        StockDetailDTO detail = stockService.findStockDetail(productId,storeId);
        return ResponseEntity.ok(detail);
    }


}
