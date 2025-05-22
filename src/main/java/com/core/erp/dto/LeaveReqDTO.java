package com.core.erp.dto;

import com.core.erp.domain.LeaveReqEntity;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LeaveReqDTO {

    private int reqId;
    private Integer empId; // FK (id만 관리)
    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd
    private Integer days;
    private String reqReason;
    private Integer reqStatus;
    private LocalDateTime createdAt;

    private MultipartFile file;

    // Entity → DTO 변환 생성자
    public LeaveReqDTO(LeaveReqEntity entity) {
        this.reqId = entity.getReqId();
        this.empId = entity.getEmployee() != null ? entity.getEmployee().getEmpId() : null;
        this.startDate = entity.getStartDate() != null ? entity.getStartDate().toString() : null;
        this.endDate = entity.getEndDate() != null ? entity.getEndDate().toString() : null;
        this.days = entity.getDays();
        this.reqReason = entity.getReqReason();
        this.reqStatus = entity.getReqStatus();
        this.createdAt = entity.getCreatedAt();
    }
}