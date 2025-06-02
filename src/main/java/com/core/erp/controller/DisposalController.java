package com.core.erp.controller;

import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.disposal.DisposalDTO;
import com.core.erp.dto.disposal.DisposalTargetDTO;
import com.core.erp.service.DisposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/erp/disposal")
@RequiredArgsConstructor
public class DisposalController {

    private final DisposalService disposalService;

    // 폐기 대상 자동 조회 API
    @GetMapping("/expired")
    public ResponseEntity<List<DisposalTargetDTO>> getExpiredItems(
            @AuthenticationPrincipal CustomPrincipal principal
    ) {
        Integer storeId = principal.getStoreId();
        List<DisposalTargetDTO> expired = disposalService.getExpiredStocksByStore(storeId);
        return ResponseEntity.ok(expired);
    }

    // 폐기 내역 조회
    @GetMapping("/history")
    public ResponseEntity<List<DisposalDTO>> getDisposalHistory(
            @RequestParam("storeId") Integer storeId
    ) {
        List<DisposalDTO> history = disposalService.getDisposalsByStore(storeId);
        return ResponseEntity.ok(history);
    }

    // 폐기 내역 조건 검색 API
    @GetMapping("/search")
    public ResponseEntity<List<DisposalDTO>> searchDisposals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ) {
        List<DisposalDTO> results = disposalService.searchDisposals(keyword, start, end);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/cancel/{disposalId}")
    public ResponseEntity<String> cancelDisposal(@PathVariable int disposalId) {
        try {
            disposalService.cancelDisposal(disposalId);
            return ResponseEntity.ok("폐기 취소 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("취소 실패: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
        }
    }

}
