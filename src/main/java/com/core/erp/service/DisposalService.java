package com.core.erp.service;

import com.core.erp.domain.DisposalEntity;
import com.core.erp.dto.disposal.DisposalDTO;
import com.core.erp.dto.disposal.DisposalTargetDTO;
import com.core.erp.dto.disposal.DisposalTargetProjection;
import com.core.erp.repository.DisposalRepository;
import com.core.erp.repository.StoreStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
        log.info("🔍 [서비스] 매장별 폐기 대상 조회 - storeId: {}", storeId);
        List<DisposalTargetProjection> results = storeStockRepository.findExpiredDisposalsByStore(storeId);
        log.info("📊 [서비스] 조회 결과 - 원시 데이터 수: {}", results.size());
        
        List<DisposalTargetDTO> mappedResults = mapToDTO(results);
        log.info("✅ [서비스] 매핑 완료 - 최종 결과 수: {}", mappedResults.size());
        
        // 첫 번째 결과가 있으면 로그로 확인
        if (!mappedResults.isEmpty()) {
            DisposalTargetDTO first = mappedResults.get(0);
            log.info("📌 [서비스] 첫 번째 결과 예시 - stockId: {}, productId: {}, proName: {}, quantity: {}", 
                    first.getStockId(), first.getProductId(), first.getProName(), first.getQuantity());
        }
        
        return mappedResults;
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
        log.info("🔍 [서비스] 매장별 폐기 내역 조회 - storeId: {}", storeId);
        
        List<DisposalEntity> disposals = disposalRepository
                .findByStoreStock_Store_StoreIdOrderByDisposalDateDesc(storeId);
        log.info("📊 [서비스] 조회 결과 - 폐기 내역 수: {}", disposals.size());
        
        List<DisposalDTO> results = disposals.stream()
                .map(DisposalDTO::new)
                .collect(Collectors.toList());
        
        // 첫 번째 결과가 있으면 로그로 확인
        if (!results.isEmpty()) {
            DisposalDTO first = results.get(0);
            log.info("📌 [서비스] 첫 번째 폐기 내역 - disposalId: {}, proName: {}, quantity: {}, date: {}", 
                    first.getDisposalId(), first.getProName(), first.getDisposalQuantity(), first.getDisposalDate());
        }
        
        return results;
    }

    // 전체 폐기 내역 조회 (본사용)
    public List<DisposalDTO> getAllDisposals() {
        log.info("🔍 [서비스] 전체 폐기 내역 조회");
        
        // 전체 폐기 데이터 확인 (디버깅용)
        List<DisposalEntity> allDisposals = disposalRepository.findAllByOrderByDisposalDateDesc();
        log.info("📊 [디버그] 전체 폐기 데이터 수: {}", allDisposals.size());
        
        // 전체 데이터 중 처음 몇 개의 점포 정보 확인
        for (int i = 0; i < Math.min(5, allDisposals.size()); i++) {
            DisposalEntity disposal = allDisposals.get(i);
            Integer disposalStoreId = disposal.getStoreStock() != null && disposal.getStoreStock().getStore() != null 
                    ? disposal.getStoreStock().getStore().getStoreId() : null;
            log.info("📋 [디버그] 폐기 {}번: storeId={}, stockId={}, proName={}", 
                    disposal.getDisposalId(), disposalStoreId, disposal.getStoreStock().getStockId(), disposal.getProName());
        }
        
        List<DisposalDTO> results = allDisposals.stream()
                .map(DisposalDTO::new)
                .collect(Collectors.toList());
        
        log.info("✅ [서비스] 전체 폐기 내역 변환 완료 - 결과 수: {}", results.size());
        
        return results;
    }

    // 키워드 & 날짜 조건 검색 (점포별)
    public List<DisposalDTO> searchDisposalsByStore(String keyword, LocalDate start, LocalDate end, Integer storeId) {
        LocalDateTime startDateTime = (start != null) ? start.atStartOfDay() : LocalDate.MIN.atStartOfDay();
        LocalDateTime endDateTime = (end != null) ? end.atTime(23, 59, 59) : LocalDate.MAX.atTime(23, 59, 59);

        // 키워드가 없는 경우 빈 문자열로 처리
        String searchKeyword = (keyword != null) ? keyword : "";

        List<DisposalEntity> disposals = disposalRepository
                .findByStoreStock_Store_StoreIdAndProduct_ProNameContainingAndDisposalDateBetweenOrderByDisposalDateDesc(
                        storeId, searchKeyword, startDateTime, endDateTime
                );

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
