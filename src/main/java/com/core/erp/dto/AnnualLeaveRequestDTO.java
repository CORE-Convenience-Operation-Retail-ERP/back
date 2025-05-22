package com.core.erp.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AnnualLeaveRequestDTO {
    private Integer empId;
    // private String reason;
    private String reason;
    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd
    private Integer days;
} 