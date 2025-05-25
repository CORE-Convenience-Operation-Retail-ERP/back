package com.core.pos.controller;

import com.core.erp.dto.sales.SalesSettleDTO;
import com.core.pos.dto.*;
import com.core.pos.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // 일별 매출 정산 API
    @PostMapping("/settlement/daily")
    public ResponseEntity<SettlementDTO> calculateDailySettlement(@RequestBody SettlementRequestDTO request) {
        try {
            SettlementDTO result = settlementService.calculateDailySettlement(request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 교대 정산 API
    @PostMapping("/settlement/shift")
    public ResponseEntity<?> calculateShift(@RequestBody ShiftSettlementRequestDTO request) {
        log.info("정산 요청 도착: {}", request);
        try {
            SettlementDTO result = settlementService.calculateShiftSettlement(request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("정산 실패: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
        }
    }

    // 월별 정신 API
    @PostMapping("/settlement/monthly")
    public ResponseEntity<SettlementDTO> createMonthlySettlement(@RequestBody MonthlySettlementRequestDTO dto) {
        SettlementDTO result = settlementService.calculateMonthlySettlement(dto);
        return ResponseEntity.ok(result);
    }

    // 연별 정신 API
    @PostMapping("/settlement/yearly")
    public ResponseEntity<SettlementDTO> createYearlySettlement(@RequestBody YearlySettlementRequestDTO dto) {
        SettlementDTO result = settlementService.calculateYearlySettlement(dto);
        return ResponseEntity.ok(result);
    }

    // 정산 미리보기 API
    @GetMapping("/settlement/preview")
    public ResponseEntity<?> getSettlementPreview(@RequestParam Integer storeId, @RequestParam String targetDate) {
        try {
            LocalDate date = LocalDate.parse(targetDate);
            SettlementPreviewDTO preview = settlementService.getPreviewSummary(storeId, date);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("정산 미리보기 실패: " + e.getMessage());
        }
    }

    // 최근 정산 이력 API
    @GetMapping("/settlement/recent/{storeId}")
    public ResponseEntity<?> getRecentSettlements(@PathVariable Integer storeId) {
        try {
            List<SettlementPreviewDTO> recentList = settlementService.getRecentSettlements(storeId);
            return ResponseEntity.ok(recentList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("최근 정산 조회 실패: " + e.getMessage());
        }
    }
}
