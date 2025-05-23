package com.core.erp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private EmployeeEntity user;
    
    // 특정 부서를 대상으로 하는 알림일 경우 해당 부서 ID (선택적)
    private Integer targetDeptId;

    // 알림 유형 상세분류
    // NOTICE: 공지사항
    // HR_JOIN: 회원가입
    // HR_LEAVE: 연차등록
    // PRODUCT_STOCK: 재고관련
    // STORE_INQUIRY: 지점문의
    private String eventType;
    
    // 기존 type 필드는 알림 표시 유형으로 사용 (INFO, WARNING, ERROR 등)
    private String type; 
    
    private String content; // 알림 내용
    
    private String link; // 알림 클릭 시 이동할 링크 (옵션)
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "is_read")
    private boolean read;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }
} 