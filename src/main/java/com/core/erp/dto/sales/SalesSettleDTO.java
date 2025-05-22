package com.core.erp.dto.sales;

import com.core.erp.domain.SalesSettleEntity;
import com.core.erp.domain.SalesSettleEntity.SettlementType;
import com.core.erp.domain.SalesSettleEntity.HqStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SalesSettleDTO {
    private Integer settlementId;
    private Integer storeId;
    private Integer empId;
    private Integer partTimerId;
    private LocalDate settlementDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;
    private Integer totalRevenue;
    private Integer discountTotal;
    private Integer refundTotal;
    private Integer finalAmount;
    private SettlementType settlementType;
    private Integer transactionCount;
    private Integer refundCount;
    private Integer isManual;
    private LocalDateTime hqSentAt;
    private HqStatus hqStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환 메소드
     */
    public static SalesSettleDTO fromEntity(SalesSettleEntity e) {
        SalesSettleDTO dto = new SalesSettleDTO();
        dto.settlementId = e.getSettlementId();
        dto.storeId = e.getStore().getStoreId();
        dto.empId = e.getEmpId();
        dto.partTimerId = e.getPartTimerId();
        dto.settlementDate = e.getSettlementDate();
        dto.startDate = e.getStartDate();
        dto.endDate = e.getEndDate();
        dto.shiftStartTime = e.getShiftStartTime();
        dto.shiftEndTime = e.getShiftEndTime();
        dto.totalRevenue = e.getTotalRevenue();
        dto.discountTotal = e.getDiscountTotal();
        dto.refundTotal = e.getRefundTotal();
        dto.finalAmount = e.getFinalAmount();
        dto.settlementType = e.getSettlementType();
        dto.transactionCount = e.getTransactionCount();
        dto.refundCount = e.getRefundCount();
        dto.isManual = e.getIsManual() != null ? e.getIsManual() : 0;
        dto.hqSentAt = e.getHqSentAt();
        dto.hqStatus = e.getHqStatus();
        dto.createdAt = e.getCreatedAt();
        dto.updatedAt = e.getUpdatedAt();
        return dto;
    }
}
