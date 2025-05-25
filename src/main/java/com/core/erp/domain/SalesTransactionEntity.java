package com.core.erp.domain;

import com.core.erp.dto.sales.SalesTransactionDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sales_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SalesTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_timer_id")
    private PartTimerEntity partTimer;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "discount_total")
    private Integer discountTotal;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "transaction_status", nullable = false)
    private Integer transactionStatus;  // 0: 완료, 1: 환불, 2: 취소, 3: 실패, 4: 승인 대기

    @Column(name = "refund_amount")
    private Integer refundAmount;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "age_group")
    private Integer ageGroup;

    @Column(name = "gender")
    private Integer gender;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<SalesDetailEntity> salesDetails;

    // 자동 created_at 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // DTO → Entity 변환 생성자
    public SalesTransactionEntity(SalesTransactionDTO dto) {
        this.transactionId = dto.getTransactionId();
        this.totalPrice = dto.getTotalPrice();
        this.discountTotal = dto.getDiscountTotal();
        this.finalAmount = dto.getFinalAmount();
        this.paymentMethod = dto.getPaymentMethod();
        this.transactionStatus = dto.getTransactionStatus();
        this.refundAmount = dto.getRefundAmount();
        this.refundReason = dto.getRefundReason();
        this.paidAt = dto.getPaidAt();
        this.refundedAt = dto.getRefundedAt();
        this.createdAt = dto.getCreatedAt();
        this.ageGroup = dto.getAgeGroup();
        this.gender = dto.getGender();
    }
}
