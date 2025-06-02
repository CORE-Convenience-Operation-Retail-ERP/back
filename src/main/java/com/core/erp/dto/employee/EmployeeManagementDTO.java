package com.core.erp.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EmployeeManagementDTO {
    private Integer empId;
    private String empName;
    private String deptCode;
    private String empStatus;
    private String empPhone;
    private String empExt;
    private String empEmail;
    private LocalDate hireDate;
    private String empImg;
    private String empAddr;

    // 은행/계좌번호 필드 추가
    private Integer empBank;
    private String empAcount;

    // 프론트엔드와 호환을 위한 추가 필드
    private String deptName;

    // 점주 관련 필드
    private String empRole;  // 구분자 (HQ: 본사직원, STORE: 점주)
    private String storeName; // 점포명
    private String storeAddr; // 점포 주소
    private String storeTel;  // 점포 전화번호
    private Integer storeId;  // 점포 ID

    public void setStoreId(int storeId) {
        this.storeId = storeId;
    }
} 