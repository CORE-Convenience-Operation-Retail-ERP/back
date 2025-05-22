package com.core.erp.controller;

import com.core.erp.domain.SalesSettleEntity.SettlementType;

import com.core.erp.dto.sales.SalesSettleDTO;
import com.core.erp.service.SalesSettleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/erp/settlement")
@RequiredArgsConstructor
public class SalesSettleController {

    private final SalesSettleService salesSettleService;

    // 점주 ERP – 정산 이력 조회 API
    @GetMapping("/list")
    public List<SalesSettleDTO> getSettlements(
            @RequestParam Integer storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type
    ) {
        // ALL 또는 type 미지정 시 모든 SettlementType을 리스트로 넘겨 조회
        if (type == null || type.trim().isEmpty() || type.equalsIgnoreCase("ALL")) {
            List<SettlementType> allTypes = Arrays.asList(SettlementType.values());
            return salesSettleService.getSettlements(storeId, startDate, endDate, allTypes);
        }

        // 특정 타입 조회
        try {
            SettlementType enumType = SettlementType.valueOf(type.toUpperCase());
            return salesSettleService.getSettlements(
                    storeId,
                    startDate,
                    endDate,
                    Collections.singletonList(enumType)
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 정산 유형입니다: " + type);
        }
    }
}
