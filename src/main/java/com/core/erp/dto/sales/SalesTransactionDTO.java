package com.core.erp.dto.sales;

import com.core.erp.domain.SalesTransactionEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SalesTransactionDTO {

    private int transactionId;
    private Integer storeId; // FK (id만 관리)
    private Integer empId; // FK (id만 관리)
    private Integer partTimerId;
    private Integer  totalPrice;
    private Integer  discountTotal;
    private Integer  finalAmount;
    private String paymentMethod;
    private Integer transactionStatus;
    private Integer refundAmount;
    private String refundReason;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime paidAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime refundedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    
    private List<SalesDetailDTO> details;
    private Integer ageGroup;
    private Integer gender;

    // 현금영수증 관련 필드
    private String receiptType;
    private String receiptIdentity;


    // Entity → DTO 변환 생성자
    public SalesTransactionDTO(SalesTransactionEntity entity, List<SalesDetailDTO> details) {
        this.transactionId = entity.getTransactionId();
        this.storeId = entity.getStore() != null ? entity.getStore().getStoreId() : null;
        this.empId = entity.getEmployee() != null ? entity.getEmployee().getEmpId() : null;
        this.partTimerId = entity.getPartTimer() != null ? entity.getPartTimer().getPartTimerId() : null;
        this.totalPrice = entity.getTotalPrice();
        this.discountTotal = entity.getDiscountTotal();
        this.finalAmount = entity.getFinalAmount();
        this.paymentMethod = entity.getPaymentMethod();
        this.transactionStatus = entity.getTransactionStatus();
        this.refundAmount = entity.getRefundAmount();
        this.refundReason = entity.getRefundReason();
        this.paidAt = entity.getPaidAt();
        this.refundedAt = entity.getRefundedAt();
        this.createdAt = entity.getCreatedAt();
        this.ageGroup = entity.getAgeGroup();
        this.gender = entity.getGender();
        this.receiptType = entity.getReceiptType();
        this.receiptIdentity = entity.getReceiptIdentity();
        this.details = details;

    }


}
