package com.core.erp.domain;

import com.core.erp.dto.LeaveReqDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_req")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LeaveReqEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "req_id")
    private int reqId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days", nullable = false)
    private int days;

    @Column(name = "req_reason", length = 255)
    private String reqReason;

    @Column(name = "req_status")
    private Integer reqStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // DTO → Entity 변환 생성자
    public LeaveReqEntity(LeaveReqDTO dto) {
        this.reqId = dto.getReqId();
        // employee는 별도 매핑 필요
        this.startDate = LocalDate.parse(dto.getStartDate());
        this.endDate = LocalDate.parse(dto.getEndDate());
        this.days = dto.getDays();
        this.reqReason = dto.getReqReason();
        this.reqStatus = dto.getReqStatus();
        this.createdAt = dto.getCreatedAt();
    }
}