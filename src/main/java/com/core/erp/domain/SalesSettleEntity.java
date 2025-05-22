package com.core.erp.domain;

import com.core.erp.dto.sales.SalesSettleDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_settlement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSettleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Integer settlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(name = "emp_id")
    private Integer empId;

    @Column(name = "part_timer_id")
    private Integer partTimerId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "shift_start_time")
    private LocalDateTime shiftStartTime;

    @Column(name = "shift_end_time")
    private LocalDateTime shiftEndTime;

    @Column(name = "total_revenue", nullable = false)
    private Integer totalRevenue;

    @Column(name = "discount_total")
    private Integer discountTotal;

    @Column(name = "refund_total")
    private Integer refundTotal;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false, length = 10)
    private SettlementType settlementType;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "refund_count")
    private Integer refundCount;

    @Column(name = "is_manual")
    private Integer isManual;

    @Column(name = "hq_sent_at")
    private LocalDateTime hqSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "hq_status", length = 10)
    private HqStatus hqStatus;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum SettlementType {
        DAILY, MONTHLY, YEARLY, SHIFT
    }

    public enum HqStatus {
        PENDING, SENT, FAILED
    }

    /**
     * DTO → Entity 변환 생성자
     */
    public SalesSettleEntity(SalesSettleDTO dto, StoreEntity storeEntity) {
        this.settlementId = dto.getSettlementId();
        this.store = storeEntity;
        this.empId = dto.getEmpId();
        this.partTimerId = dto.getPartTimerId();
        this.settlementDate = dto.getSettlementDate();
        this.startDate = dto.getStartDate();
        this.endDate = dto.getEndDate();
        this.shiftStartTime = dto.getShiftStartTime();
        this.shiftEndTime = dto.getShiftEndTime();
        this.totalRevenue = dto.getTotalRevenue();
        this.discountTotal = dto.getDiscountTotal();
        this.refundTotal = dto.getRefundTotal();
        this.finalAmount = dto.getFinalAmount();
        this.settlementType = dto.getSettlementType();
        this.transactionCount = dto.getTransactionCount();
        this.refundCount = dto.getRefundCount();
        this.isManual = dto.getIsManual();
        this.hqSentAt = dto.getHqSentAt();
        this.hqStatus = dto.getHqStatus();
        this.createdAt = dto.getCreatedAt();
        this.updatedAt = dto.getUpdatedAt();
    }
}
