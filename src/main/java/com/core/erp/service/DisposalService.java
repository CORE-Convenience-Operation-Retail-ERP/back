package com.core.erp.service;

import com.core.erp.domain.DisposalEntity;
import com.core.erp.dto.disposal.DisposalDTO;
import com.core.erp.dto.disposal.DisposalTargetDTO;
import com.core.erp.dto.disposal.DisposalTargetProjection;
import com.core.erp.repository.DisposalRepository;
import com.core.erp.repository.StoreStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisposalService {

    private final StoreStockRepository storeStockRepository;
    private final DisposalRepository disposalRepository;

    // 유통기한 지난 재고 전체 조회
    public List<DisposalTargetDTO> getExpiredStocks() {
        List<DisposalTargetProjection> results = storeStockRepository.findExpiredDisposals();
        return mapToDTO(results);
    }

    // 매장별 유통기한 지난 재고 조회
    public List<DisposalTargetDTO> getExpiredStocksByStore(Integer storeId) {
        List<DisposalTargetProjection> results = storeStockRepository.findExpiredDisposalsByStore(storeId);
        return mapToDTO(results);
    }

    // 공통 DTO 매핑 함수
    private List<DisposalTargetDTO> mapToDTO(List<DisposalTargetProjection> projections) {
        return projections.stream()
                .map(p -> new DisposalTargetDTO(
                        p.getStockId(),
                        p.getProductId(),
                        p.getProName(),
                        p.getQuantity(),
                        p.getLastInDate(),
                        p.getExpiredDate()
                ))
                .collect(Collectors.toList());
    }

    // 매장별 폐기 내역 조회
    public List<DisposalDTO> getDisposalsByStore(Integer storeId) {
        List<DisposalEntity> disposals = disposalRepository
                .findByStoreStock_Store_StoreIdOrderByDisposalDateDesc(storeId);
        return disposals.stream()
                .map(DisposalDTO::new)
                .collect(Collectors.toList());
    }

    // 키워드 & 날짜 조건 검색
    public List<DisposalDTO> searchDisposals(String keyword, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = (start != null) ? start.atStartOfDay() : LocalDate.MIN.atStartOfDay();
        LocalDateTime endDateTime = (end != null) ? end.atTime(23, 59, 59) : LocalDate.MAX.atTime(23, 59, 59);

        List<DisposalEntity> disposals = disposalRepository
                .findByProduct_ProNameContainingAndDisposalDateBetweenOrderByDisposalDateDesc(
                        keyword, startDateTime, endDateTime
                );

        return disposals.stream()
                .map(DisposalDTO::new)
                .collect(Collectors.toList());
    }

    // 폐기 취소 처리
    @Transactional
    public void cancelDisposal(int disposalId) {
        DisposalEntity disposal = disposalRepository.findById(disposalId)
                .orElseThrow(() -> new IllegalArgumentException("폐기 내역을 찾을 수 없습니다."));

        Long productId = Long.valueOf(disposal.getProduct().getProductId());
        Integer storeId = disposal.getStoreStock().getStore().getStoreId();
        int qty = disposal.getDisposalQuantity();

        storeStockRepository.increaseQuantityAndUpdateDate(productId, storeId, qty);
        disposalRepository.delete(disposal);
    }
}
