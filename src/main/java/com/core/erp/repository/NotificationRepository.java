package com.core.erp.repository;

import com.core.erp.domain.EmployeeEntity;
import com.core.erp.domain.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    
    // 특정 사용자의 알림 목록 조회
    List<NotificationEntity> findByUserOrderByCreatedAtDesc(EmployeeEntity user);
    
    // 특정 사용자의 읽지 않은 알림 목록 조회
    List<NotificationEntity> findByUserAndReadFalseOrderByCreatedAtDesc(EmployeeEntity user);
    
    // 특정 사용자의 알림 페이지네이션 조회
    List<NotificationEntity> findByUserOrderByCreatedAtDesc(EmployeeEntity user, Pageable pageable);
    
    // 읽지 않은 알림 개수 조회
    long countByUserAndReadFalse(EmployeeEntity user);
    
    // 특정 부서의 알림 목록 조회
    List<NotificationEntity> findByTargetDeptIdOrderByCreatedAtDesc(Integer targetDeptId, Pageable pageable);
    
    // 특정 이벤트 유형의 알림 목록 조회
    List<NotificationEntity> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);
    
    // 특정 부서의 읽지 않은 알림 개수 조회
    long countByTargetDeptIdAndReadFalse(Integer targetDeptId);
    
    // 특정 이벤트 유형의 읽지 않은 알림 개수 조회
    long countByEventTypeAndReadFalse(String eventType);
} 