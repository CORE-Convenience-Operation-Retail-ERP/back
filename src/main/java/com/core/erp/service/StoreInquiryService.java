package com.core.erp.service;

import com.core.erp.domain.StoreEntity;
import com.core.erp.domain.StoreInquiryEntity;
import com.core.erp.dto.store.StoreInquiryDTO;
import com.core.erp.repository.StoreInquiryRepository;
import com.core.erp.repository.StoreRepository;
import com.core.erp.repository.EmployeeRepository;
import com.core.erp.domain.EmployeeEntity;
import com.core.erp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class StoreInquiryService {

    private final StoreInquiryRepository inquiryRepository;
    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    
    // 모든 문의 조회 (페이징 처리)
    public Page<StoreInquiryDTO> getAllInquiriesWithPaging(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inqCreatedAt").descending());
        return inquiryRepository.findAll(pageable)
                .map(StoreInquiryDTO::new);
    }
    
    // 모든 문의 조회
    public List<StoreInquiryDTO> getAllInquiries() {
        return inquiryRepository.findAll().stream()
                .map(StoreInquiryDTO::new)
                .collect(Collectors.toList());
    }
    
    // 특정 문의 조회
    public StoreInquiryDTO getInquiryById(int inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .map(StoreInquiryDTO::new)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다. ID: " + inquiryId));
    }
    
    // 특정 지점의 문의 조회 (페이징 처리)
    public Page<StoreInquiryDTO> getInquiriesByStoreIdWithPaging(Integer storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inqCreatedAt").descending());
        return inquiryRepository.findByStore_StoreId(storeId, pageable)
                .map(StoreInquiryDTO::new);
    }
    
    // 특정 지점의 문의 조회
    public List<StoreInquiryDTO> getInquiriesByStoreId(Integer storeId) {
        return inquiryRepository.findByStore_StoreId(storeId).stream()
                .map(StoreInquiryDTO::new)
                .collect(Collectors.toList());
    }
    
    // 특정 유형별 문의 조회 (페이징 처리)
    public Page<StoreInquiryDTO> getInquiriesByTypeWithPaging(int type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inqCreatedAt").descending());
        return inquiryRepository.findByInqType(type, pageable)
                .map(StoreInquiryDTO::new);
    }
    
    // 특정 유형별 문의 조회
    public List<StoreInquiryDTO> getInquiriesByType(int type) {
        return inquiryRepository.findByInqType(type).stream()
                .map(StoreInquiryDTO::new)
                .collect(Collectors.toList());
    }
    
    // 특정 상태별 문의 조회 (페이징 처리)
    public Page<StoreInquiryDTO> getInquiriesByStatusWithPaging(int status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inqCreatedAt").descending());
        return inquiryRepository.findByInqStatus(status, pageable)
                .map(StoreInquiryDTO::new);
    }
    
    // 특정 상태별 문의 조회
    public List<StoreInquiryDTO> getInquiriesByStatus(int status) {
        return inquiryRepository.findByInqStatus(status).stream()
                .map(StoreInquiryDTO::new)
                .collect(Collectors.toList());
    }
    
    // 지점 ID와 유형으로 문의 조회 (페이징 처리)
    public Page<StoreInquiryDTO> getInquiriesByStoreIdAndTypeWithPaging(Integer storeId, int type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inqCreatedAt").descending());
        return inquiryRepository.findByStoreIdAndType(storeId, type, pageable)
                .map(StoreInquiryDTO::new);
    }
    
    // 지점 ID와 유형으로 문의 조회
    public List<StoreInquiryDTO> getInquiriesByStoreIdAndType(Integer storeId, int type) {
        return inquiryRepository.findByStoreIdAndType(storeId, type).stream()
                .map(StoreInquiryDTO::new)
                .collect(Collectors.toList());
    }
    
    // 지점 ID와 상태로 문의 조회 (페이징 처리)
    public Page<StoreInquiryDTO> getInquiriesByStoreIdAndStatusWithPaging(Integer storeId, int status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("inqCreatedAt").descending());
        return inquiryRepository.findByStoreIdAndStatus(storeId, status, pageable)
                .map(StoreInquiryDTO::new);
    }
    
    // 지점 ID와 상태로 문의 조회
    public List<StoreInquiryDTO> getInquiriesByStoreIdAndStatus(Integer storeId, int status) {
        return inquiryRepository.findByStoreIdAndStatus(storeId, status).stream()
                .map(StoreInquiryDTO::new)
                .collect(Collectors.toList());
    }
    
    // 새 문의 등록
    @Transactional
    public StoreInquiryDTO createInquiry(StoreInquiryDTO inquiryDTO) {
        StoreEntity store = storeRepository.findById(inquiryDTO.getStoreId())
                .orElseThrow(() -> new RuntimeException("지점을 찾을 수 없습니다. ID: " + inquiryDTO.getStoreId()));
        
        StoreInquiryEntity entity = new StoreInquiryEntity(inquiryDTO, store);
        entity.setInqCreatedAt(LocalDateTime.now());
        entity.setInqStatus(2); // 기본 상태: 대기(2)
        
        StoreInquiryEntity savedEntity = inquiryRepository.save(entity);
        // 알림 생성 코드 추가
        System.out.println("[알림] 문의 등록 알림 코드 진입");
        try {
            // 지점관리팀+MASTER 알림 대상
            List<EmployeeEntity> targets = new ArrayList<>();
            targets.addAll(employeeRepository.findByDepartment_DeptId(8));
            List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10);
            for (EmployeeEntity master : masters) {
                if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                    targets.add(master);
                }
            }
            for (EmployeeEntity target : targets) {
                notificationService.createNotification(
                    target.getEmpId(),
                    8,
                    "STORE_INQUIRY",
                    "INFO",
                    "[지점문의] 새 문의가 등록되었습니다.",
                    "/headquarters/store/inquiry"
                );
            }
        } catch (Exception e) {
            System.out.println("[알림] 문의 등록 알림 예외: " + e.getMessage());
            e.printStackTrace();
        }
        return new StoreInquiryDTO(savedEntity);
    }
    
    // 문의 상태 업데이트
    @Transactional
    public StoreInquiryDTO updateInquiryStatus(int inquiryId, int status) {
        StoreInquiryEntity entity = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다. ID: " + inquiryId));
        
        entity.setInqStatus(status);
        StoreInquiryEntity savedEntity = inquiryRepository.save(entity);
        return new StoreInquiryDTO(savedEntity);
    }
    
    // 문의 평가 등급 업데이트
    @Transactional
    public StoreInquiryDTO updateInquiryLevel(int inquiryId, Integer level) {
        StoreInquiryEntity entity = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다. ID: " + inquiryId));
        
        entity.setInqLevel(level);
        StoreInquiryEntity savedEntity = inquiryRepository.save(entity);
        return new StoreInquiryDTO(savedEntity);
    }
    
    // 지점별 평가 점수 통계 조회 (지점 순위 매기기용)
    public List<Map<String, Object>> getStoreRanking() {
        // 모든 문의 가져오기
        List<StoreInquiryEntity> allInquiries = inquiryRepository.findAll();
        
        // 지점별 점수 집계를 위한 맵
        Map<Integer, Map<String, Object>> storeScores = new HashMap<>();
        
        for (StoreInquiryEntity inquiry : allInquiries) {
            if (inquiry.getStore() == null || inquiry.getInqLevel() == null) {
                continue; // 지점 정보나 평가가 없는 경우 건너뛰기
            }
            
            int storeId = inquiry.getStore().getStoreId();
            String storeName = inquiry.getStore().getStoreName();
            
            // 지점 정보 초기화
            if (!storeScores.containsKey(storeId)) {
                Map<String, Object> storeData = new HashMap<>();
                storeData.put("storeId", storeId);
                storeData.put("storeName", storeName);
                storeData.put("complaintCount", 0);
                storeData.put("complaintScore", 0);
                storeData.put("praiseCount", 0);
                storeData.put("praiseScore", 0);
                storeData.put("totalScore", 0.0);
                storeScores.put(storeId, storeData);
            }
            
            Map<String, Object> storeData = storeScores.get(storeId);
            
            // 문의 유형에 따라 점수 집계
            if (inquiry.getInqType() == 1) { // 컴플레인
                int count = (int) storeData.get("complaintCount") + 1;
                int score = (int) storeData.get("complaintScore") + inquiry.getInqLevel();
                storeData.put("complaintCount", count);
                storeData.put("complaintScore", score);
            } else if (inquiry.getInqType() == 2) { // 칭찬
                int count = (int) storeData.get("praiseCount") + 1;
                int score = (int) storeData.get("praiseScore") + inquiry.getInqLevel();
                storeData.put("praiseCount", count);
                storeData.put("praiseScore", score);
            }
            
            // 총점 계산 (칭찬은 양수, 컴플레인은 음수로 계산)
            double totalScore = 0.0;
            int praiseScore = (int) storeData.get("praiseScore");
            int complaintScore = (int) storeData.get("complaintScore");
            
            // 칭찬 점수는 그대로 반영
            if ((int) storeData.get("praiseCount") > 0) {
                totalScore += praiseScore;
            }
            
            // 컴플레인 점수는 음수로 반영 (가중치 적용: 심각도 5는 -10점)
            if ((int) storeData.get("complaintCount") > 0) {
                totalScore -= complaintScore; // 가중치 없이 1배만 반영
            }
            
            storeData.put("totalScore", totalScore);
        }
        
        // 지점 순위 리스트 생성
        List<Map<String, Object>> storeRankings = new ArrayList<>(storeScores.values());
        
        // 총점 기준으로 내림차순 정렬
        storeRankings.sort((a, b) -> {
            double scoreA = (double) a.get("totalScore");
            double scoreB = (double) b.get("totalScore");
            return Double.compare(scoreB, scoreA); // 내림차순
        });
        
        // 등수 추가
        for (int i = 0; i < storeRankings.size(); i++) {
            storeRankings.get(i).put("rank", i + 1);
        }
        
        return storeRankings;
    }
} 