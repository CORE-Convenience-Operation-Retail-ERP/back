package com.core.pos.dto;

import com.core.pos.domain.SalesSettlementEntity;
import com.core.pos.domain.SalesSettlementEntity.HqStatus;
import com.core.pos.domain.SalesSettlementEntity.SettlementType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementResultDTO {

    private Integer settlementId;
    private Integer storeId;
    private Integer empId;
    private Integer partTimerId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate settlementDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime shiftStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime shiftEndTime;

    private Integer totalRevenue;
    private Integer discountTotal;
    private Integer refundTotal;
    private Integer finalAmount;

    private SettlementType settlementType;
    private Integer transactionCount;
    private Integer refundCount;

    private Integer isManual;
    private HqStatus hqStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime hqSentAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;

    // 변환 메서드
    public static SettlementResultDTO from(SalesSettlementEntity entity) {
        return SettlementResultDTO.builder()
                .settlementId(entity.getSettlementId())
                .storeId(entity.getStoreId())
                .empId(entity.getEmpId())
                .partTimerId(entity.getPartTimerId())
                .settlementDate(entity.getSettlementDate())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .shiftStartTime(entity.getShiftStartTime())
                .shiftEndTime(entity.getShiftEndTime())
                .totalRevenue(entity.getTotalRevenue())
                .discountTotal(entity.getDiscountTotal())
                .refundTotal(entity.getRefundTotal())
                .finalAmount(entity.getFinalAmount())
                .settlementType(entity.getSettlementType())
                .transactionCount(entity.getTransactionCount())
                .refundCount(entity.getRefundCount())
                .isManual(entity.getIsManual())
                .hqStatus(entity.getHqStatus())
                .hqSentAt(entity.getHqSentAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
