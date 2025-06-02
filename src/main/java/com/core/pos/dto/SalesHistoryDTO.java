package com.core.pos.dto;

import com.core.erp.domain.SalesTransactionEntity;
import com.core.erp.dto.sales.SalesDetailDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SalesHistoryDTO {

    private Integer transactionId;
    private String paymentMethod;
    private Integer finalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime paidAt;

    // 환불 관련 필드
    private Integer transactionStatus;
    private Integer refundAmount;
    private String refundReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime refundedAt;

    // 상품 요약 정보
    private Integer totalPrice;
    private Integer discountTotal;
    private List<SalesDetailDTO> items;

    // 변환 생성자
    public SalesHistoryDTO(SalesTransactionEntity entity, List<SalesDetailDTO> items) {
        this.transactionId = entity.getTransactionId();
        this.paymentMethod = entity.getPaymentMethod();
        this.totalPrice = entity.getTotalPrice();
        this.discountTotal = entity.getDiscountTotal();
        this.finalAmount = entity.getFinalAmount();
        this.paidAt = entity.getPaidAt();
        this.transactionStatus = entity.getTransactionStatus();
        this.refundAmount = entity.getRefundAmount();
        this.refundReason = entity.getRefundReason();
        this.refundedAt = entity.getRefundedAt();
        this.items = items;

    }
}