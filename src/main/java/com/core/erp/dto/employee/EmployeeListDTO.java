package com.core.erp.dto.employee;

import lombok.Data;

@Data
public class EmployeeListDTO {
    private Long empId;
    private String empName;
    private String deptName;
    private String empRole;
    private String empStatus;
    private String hireDate;
    private String totalWorkDays;
    private String daysWorked;
    private String empEmail;
    private String empPhone;
    private String empExt;
    private String empAddr;
    private String empImg;
    // 매장 정보 필드 추가
    private Long storeId;
    private String storeName;
    private String storeAddr;
    private String storeTel;
    // 필요시 추가
} 