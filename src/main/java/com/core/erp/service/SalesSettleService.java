package com.core.erp.service;

import com.core.erp.domain.SalesSettleEntity;
import com.core.erp.domain.SalesSettleEntity.SettlementType;
import com.core.erp.dto.sales.SalesSettleDTO;
import com.core.erp.repository.SalesSettleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesSettleService {

    private final SalesSettleRepository salesSettleRepository;

    // 정산 이력 조회 (전체 타입 또는 특정 타입 목록)
    public List<SalesSettleDTO> getSettlements(
            Integer storeId,
            LocalDate startDate,
            LocalDate endDate,
            List<SettlementType> types
    ) {
        // null 또는 빈 리스트면 전체 SettlementType으로 대체
        if (types == null || types.isEmpty()) {
            types = Arrays.asList(SettlementType.values());
        }

        // 기본 날짜 범위 설정 (필요 시 default 적용)
        // null startDate -> 매우 과거, null endDate -> 오늘
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        // 정렬된 엔티티 조회 (날짜 오름차순)
        List<SalesSettleEntity> entities = salesSettleRepository
                .findByStore_StoreIdAndSettlementDateBetweenAndSettlementTypeInOrderBySettlementDateAsc(
                        storeId, start, end, types
                );
        // Entity -> DTO 변환
        return entities.stream()
                .map(SalesSettleDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
