package com.core.erp.domain;

import com.core.erp.dto.salary.SalaryDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "salary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SalaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_id")
    private int salaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_timer_id")
    private PartTimerEntity partTimer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private StoreEntity store;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "base_salary", nullable = false)
    private int baseSalary;

    @Column(name = "bonus", nullable = false)
    private int bonus;

    @Column(name = "deduct_total", nullable = false)
    private int deductTotal;

    @Column(name = "deduct_extra")
    private Integer deductExtra;

    @Column(name = "net_salary", nullable = false)
    private int netSalary;

    @Column(name = "pay_date", nullable = false)
    private LocalDateTime payDate;

    @Column(name = "pay_status", nullable = false)
    private int payStatus;

    // DTO → Entity 변환 생성자
    public SalaryEntity(SalaryDTO dto) {
        this.salaryId = dto.getSalaryId();
        // employee, partTimer, store는 별도 매핑 필요
        this.calculatedAt = dto.getCalculatedAt();
        this.baseSalary = dto.getBaseSalary();
        this.bonus = dto.getBonus();
        this.deductTotal = dto.getDeductTotal();
        this.deductExtra = dto.getDeductExtra();
        this.netSalary = dto.getNetSalary();
        this.payDate = dto.getPayDate();
        this.payStatus = dto.getPayStatus();
    }
}