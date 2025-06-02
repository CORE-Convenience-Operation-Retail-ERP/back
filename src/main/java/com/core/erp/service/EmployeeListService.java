package com.core.erp.service;

import com.core.erp.dto.employee.EmployeeListDTO;
import com.core.erp.domain.EmployeeEntity;
import com.core.erp.domain.DepartmentEntity;
import com.core.erp.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeListService {
    private final EmployeeRepository employeeRepository;

    /**
     * 직원의 총 재직일수를 계산합니다.
     * 입사일부터 현재까지의 달력상 일수를 계산합니다.
     * 
     * @param hireDate 직원 입사일자
     * @return 총 재직일수 (문자열 형태, '일' 단위 포함)
     */
    public String calculateWorkDays(LocalDateTime hireDate) {
        if (hireDate == null) {
            return "0일";
        }
        
        try {
            // 현재 날짜
            LocalDate currentDate = LocalDate.now();
            // 입사일 (시간 정보 제외)
            LocalDate hireDateOnly = hireDate.toLocalDate();
            
            // 입사일과 현재 날짜 사이의 일수 계산 (양 끝 날짜 포함)
            long daysBetween = ChronoUnit.DAYS.between(hireDateOnly, currentDate) + 1;
            
            // '일' 단위를 추가하여 반환
            return daysBetween + "일";
        } catch (Exception e) {
            // 날짜 변환이나 계산 중 오류 발생 시 0일 반환
            return "0일";
        }
    }
    
    /**
     * 입사일을 'YYYY년 MM월 DD일' 형식으로 변환합니다.
     * 
     * @param hireDate 입사일 (LocalDateTime)
     * @return 형식화된 입사일 문자열
     */
    public String formatHireDate(LocalDateTime hireDate) {
        if (hireDate == null) {
            return "";
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
            return hireDate.format(formatter);
        } catch (Exception e) {
            // 날짜 형식 변환 중 오류 발생 시 빈 문자열 반환
            return "";
        }
    }
    
    /**
     * 부서 코드를 사용자 친화적인 이름으로 변환합니다.
     * 
     * @param deptCode 부서 코드
     * @return 부서명
     */
    public String getDepartmentDisplayName(String deptCode) {
        if (deptCode == null) {
            return "미승인 사원";
        }
        
        switch (deptCode) {
            case "HQ_HRM":
                return "인사팀";
            case "HQ_HRM_M":
                return "인사팀";    
            case "HQ_BR":
                return "지점관리팀";
            case "HQ_BR_M":
                return "지점관리팀";
            case "HQ_PRO":
                return "상품관리팀";
            case "HQ_PRO_M":
                return "상품관리팀";
            default:
                return deptCode;
        }
    }
    
    /**
     * 상태 코드를 표시 이름으로 변환합니다.
     * 
     * @param statusCode 상태 코드
     * @return 상태 표시명
     */
    public String getStatusDisplayName(String statusCode) {
        if (statusCode == null) {
            return "미승인 사원";
        }
        
        switch (statusCode) {
            case "1":
                return "재직";
            case "2":
                return "휴직";
            case "3":
                return "퇴사";
            default:
                return statusCode;
        }
    }

    public List<EmployeeListDTO> getEmployeeLists(String deptName, String empName, String empId, String sort, String order, String empType) {
        List<EmployeeEntity> employees = employeeRepository.findAll(); // 실제 환경에서는 동적 쿼리로 교체

        return employees.stream()
                .filter(e -> {
                    // empType 필터링 (HQ = 본사 직원만, STORE = 점주만)
                    if ("HQ".equals(empType)) {
                        // 본사 직원: empRole이 '본사'인 경우만 포함
                        return "본사".equals(e.getEmpRole());
                    } else if ("STORE".equals(empType)) {
                        // 점주: empRole이 '점주'인 경우만 포함
                        return "점주".equals(e.getEmpRole());
                    }
                    return true; // empType이 지정되지 않으면 모든 직원 반환
                })
                .filter(e -> deptName == null || deptName.isEmpty() || (e.getDepartment() != null && e.getDepartment().getDeptName() != null && e.getDepartment().getDeptName().equals(deptName)))
                .filter(e -> empName == null || empName.isEmpty() || e.getEmpName().contains(empName))
                .filter(e -> empId == null || empId.isEmpty() || Integer.toString(e.getEmpId()).equals(empId))
                .sorted((a, b) -> {
                    if ("empId".equals(sort)) {
                        return "desc".equals(order) ? Integer.compare(b.getEmpId(), a.getEmpId()) : Integer.compare(a.getEmpId(), b.getEmpId());
                    }
                    return 0;
                })
                .map(e -> {
                    EmployeeListDTO dto = new EmployeeListDTO();
                    dto.setEmpId((long) e.getEmpId());
                    dto.setEmpName(e.getEmpName() != null ? e.getEmpName() : "");
                    
                    // 부서 코드를 사용자 친화적인 이름으로 변환
                    String deptDisplayName = "";
                    if (e.getDepartment() != null && e.getDepartment().getDeptName() != null) {
                        deptDisplayName = getDepartmentDisplayName(e.getDepartment().getDeptName());
                    } else {
                        deptDisplayName = getDepartmentDisplayName(null);
                    }
                    dto.setDeptName(deptDisplayName);
                    
                    dto.setEmpRole(e.getEmpRole() != null ? e.getEmpRole() : "");
                    
                    // 상태 코드를 표시 이름으로 변환
                    String statusDisplayName = getStatusDisplayName(e.getEmpStatus());
                    dto.setEmpStatus(statusDisplayName);
                    
                    // 입사일 포맷팅
                    dto.setHireDate(formatHireDate(e.getHireDate()));
                    
                    // 재직일수 계산 및 설정
                    String workDays = calculateWorkDays(e.getHireDate());
                    dto.setTotalWorkDays(workDays);
                    
                    // 프론트엔드와 호환성을 위해 daysWorked 필드도 설정
                    dto.setDaysWorked(workDays);
                    
                    // 이메일 정보 설정 (loginId)
                    dto.setEmpEmail(e.getLoginId() != null ? e.getLoginId() : "");
                    
                    // 연락처 정보 설정
                    dto.setEmpPhone(e.getEmpPhone() != null ? e.getEmpPhone() : "");
                    
                    // 내선번호 설정
                    dto.setEmpExt(e.getEmpExt() != null ? e.getEmpExt().toString() : "");
                    
                    // 주소 정보 설정
                    dto.setEmpAddr(e.getEmpAddr() != null ? e.getEmpAddr() : "");
                    
                    // 프로필 이미지 설정 추가
                    dto.setEmpImg(e.getEmpImg());
                    
                    // 매장 정보 설정 (점주의 경우)
                    if (e.getStore() != null) {
                        dto.setStoreId((long) e.getStore().getStoreId());
                        dto.setStoreName(e.getStore().getStoreName());
                        dto.setStoreAddr(e.getStore().getStoreAddr());
                        dto.setStoreTel(e.getStore().getStoreTel());
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public EmployeeListDTO getEmployeeListDetail(Long empId) {
        EmployeeEntity e = employeeRepository.findById(empId.intValue()).orElseThrow();
        EmployeeListDTO dto = new EmployeeListDTO();
        
        try {
            dto.setEmpId((long) e.getEmpId());
            dto.setEmpName(e.getEmpName() != null ? e.getEmpName() : "");
            dto.setEmpRole(e.getEmpRole() != null ? e.getEmpRole() : "");
            
            // 상태 코드를 표시 이름으로 변환
            String statusDisplayName = getStatusDisplayName(e.getEmpStatus());
            dto.setEmpStatus(statusDisplayName);
            
            // 입사일 포맷팅
            dto.setHireDate(formatHireDate(e.getHireDate()));
            
            // 부서 코드를 사용자 친화적인 이름으로 변환
            DepartmentEntity d = e.getDepartment();
            String deptDisplayName = "";
            if (d != null && d.getDeptName() != null) {
                deptDisplayName = getDepartmentDisplayName(d.getDeptName());
            } else {
                deptDisplayName = getDepartmentDisplayName(null);
            }
            dto.setDeptName(deptDisplayName);
            
            // 재직일수 계산 및 설정
            String workDays = calculateWorkDays(e.getHireDate());
            dto.setTotalWorkDays(workDays);
            
            // 프론트엔드와 호환성을 위해 daysWorked 필드도 설정
            dto.setDaysWorked(workDays);
            
            // 이메일 정보 설정 (loginId)
            dto.setEmpEmail(e.getLoginId() != null ? e.getLoginId() : "");
            
            // 연락처 정보 설정
            dto.setEmpPhone(e.getEmpPhone() != null ? e.getEmpPhone() : "");
            
            // 내선번호 설정
            dto.setEmpExt(e.getEmpExt() != null ? e.getEmpExt().toString() : "");
            
            // 주소 정보 설정
            dto.setEmpAddr(e.getEmpAddr() != null ? e.getEmpAddr() : "");
            
            // 프로필 이미지 설정 추가
            dto.setEmpImg(e.getEmpImg());
            
            // 매장 정보 설정 (점주의 경우)
            if (e.getStore() != null) {
                dto.setStoreId((long) e.getStore().getStoreId());
                dto.setStoreName(e.getStore().getStoreName());
                dto.setStoreAddr(e.getStore().getStoreAddr());
                dto.setStoreTel(e.getStore().getStoreTel());
            }
        } catch (Exception ex) {
            // 예외 발생 시 기본값 설정
            if (dto.getEmpName() == null) dto.setEmpName("");
            if (dto.getDeptName() == null) dto.setDeptName(getDepartmentDisplayName(null));
            if (dto.getEmpRole() == null) dto.setEmpRole("");
            if (dto.getEmpStatus() == null) dto.setEmpStatus(getStatusDisplayName(null));
            if (dto.getHireDate() == null) dto.setHireDate("");
            if (dto.getTotalWorkDays() == null) dto.setTotalWorkDays("0일");
            if (dto.getDaysWorked() == null) dto.setDaysWorked("0일");
            if (dto.getEmpEmail() == null) dto.setEmpEmail("");
            if (dto.getEmpPhone() == null) dto.setEmpPhone("");
            if (dto.getEmpExt() == null) dto.setEmpExt("");
            if (dto.getEmpAddr() == null) dto.setEmpAddr("");
        }
        
        return dto;
    }
} 