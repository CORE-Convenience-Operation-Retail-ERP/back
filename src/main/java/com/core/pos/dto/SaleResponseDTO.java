package com.core.pos.dto;

import com.core.erp.domain.SalesTransactionEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SaleResponseDTO {

    private Integer transactionId;
    private Integer storeId;
    private Integer empId;
    private Integer totalPrice;
    private Integer discountTotal;
    private Integer finalAmount;
    private String paymentMethod;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime paidAt;
    
    private Integer transactionStatus;

    public SaleResponseDTO(SalesTransactionEntity entity) {
        this.transactionId = entity.getTransactionId();
        this.storeId = entity.getStore().getStoreId();
        this.empId = entity.getEmployee() != null ? entity.getEmployee().getEmpId() : null;
        this.totalPrice = entity.getTotalPrice();
        this.discountTotal = entity.getDiscountTotal();
        this.finalAmount = entity.getFinalAmount();
        this.paymentMethod = entity.getPaymentMethod();
        this.paidAt = entity.getPaidAt();
        this.transactionStatus = entity.getTransactionStatus();
    }
}