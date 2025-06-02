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
public class RefundResponseDTO {

    private Integer transactionId;
    private String refundReason;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime refundedAt;
    
    private String statusMessage;

    public RefundResponseDTO(SalesTransactionEntity entity) {
        this.transactionId = entity.getTransactionId();
        this.refundReason = entity.getRefundReason();
        this.refundedAt = entity.getRefundedAt();
        this.statusMessage = "환불 완료"; // 필요 시 외부에서 설정 가능
    }
}
