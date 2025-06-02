package com.core.erp.controller;

import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.disposal.DisposalDTO;
import com.core.erp.dto.disposal.DisposalTargetDTO;
import com.core.erp.service.DisposalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/erp/disposal")
@RequiredArgsConstructor
@Slf4j
public class DisposalController {

    private final DisposalService disposalService;

    // 폐기 대상 자동 조회 API
    @GetMapping("/expired")
    public ResponseEntity<List<DisposalTargetDTO>> getExpiredItems(
            @AuthenticationPrincipal CustomPrincipal principal
    ) {
        log.info("🔍 [폐기 대상 조회] 사용자 정보 - empId: {}, deptId: {}, storeId: {}, role: {}", 
                principal.getEmpId(), principal.getDeptId(), principal.getStoreId(), principal.getRole());
        
        Integer storeId = principal.getStoreId();
        List<DisposalTargetDTO> expired;
        
        if (storeId == null) {
            log.warn("⚠️ [폐기 대상 조회] storeId가 null이므로 전체 데이터를 조회합니다.");
            expired = disposalService.getExpiredStocks(); // 전체 조회
        } else {
            expired = disposalService.getExpiredStocksByStore(storeId); // 점포별 조회
        }
        
        log.info("✅ [폐기 대상 조회] storeId: {}, 조회된 데이터 수: {}", storeId, expired.size());
        return ResponseEntity.ok(expired);
    }

    // 폐기 내역 조회
    @GetMapping("/history")
    public ResponseEntity<List<DisposalDTO>> getDisposalHistory(
            @AuthenticationPrincipal CustomPrincipal principal
    ) {
        log.info("🔍 [폐기 내역 조회] 사용자 정보 - empId: {}, deptId: {}, storeId: {}, role: {}", 
                principal.getEmpId(), principal.getDeptId(), principal.getStoreId(), principal.getRole());
        
        Integer storeId = principal.getStoreId();
        List<DisposalDTO> history;
        
        if (storeId == null) {
            log.warn("⚠️ [폐기 내역 조회] storeId가 null이므로 전체 데이터를 조회합니다.");
            history = disposalService.getAllDisposals(); // 전체 조회
        } else {
            history = disposalService.getDisposalsByStore(storeId); // 점포별 조회
        }
        
        log.info("✅ [폐기 내역 조회] storeId: {}, 조회된 데이터 수: {}", storeId, history.size());
        return ResponseEntity.ok(history);
    }

    // 폐기 내역 조건 검색 API
    @GetMapping("/search")
    public ResponseEntity<List<DisposalDTO>> searchDisposals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @AuthenticationPrincipal CustomPrincipal principal
    ) {
        Integer storeId = principal.getStoreId();
        List<DisposalDTO> results = disposalService.searchDisposalsByStore(keyword, start, end, storeId);
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
