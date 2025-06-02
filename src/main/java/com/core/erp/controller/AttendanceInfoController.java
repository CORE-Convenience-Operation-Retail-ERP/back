package com.core.erp.controller;

import com.core.erp.dto.AttendanceInfoDTO;
import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.employee.EmployeeDTO;
import com.core.erp.service.AttendanceInfoService;
import com.core.erp.domain.EmployeeEntity;
import com.core.erp.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class AttendanceInfoController {

    private final AttendanceInfoService attendanceInfoService;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/my-page")
    public Map<String, Object> getMyAttendanceInfo(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        String loginId = principal.getLoginId();

        System.out.println("마이페이지 API 호출: 로그인ID = " + loginId);

        EmployeeEntity emp = employeeRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사원 정보 없음"));
        
        System.out.println("사원 정보 조회 성공: 사원ID = " + emp.getEmpId() + ", 이름 = " + emp.getEmpName());

        // 근태 정보 조회
        AttendanceInfoDTO attendanceInfo = attendanceInfoService.getEmployeeAttendanceInfo(emp.getEmpId());
        
        System.out.println("근태 정보 조회 결과:");
        System.out.println("- 근무일수: " + (attendanceInfo != null ? attendanceInfo.getAttendanceDays() : "null"));
        System.out.println("- 지각횟수: " + (attendanceInfo != null ? attendanceInfo.getLateCount() : "null"));
        System.out.println("- 결근횟수: " + (attendanceInfo != null ? attendanceInfo.getAbsentCount() : "null"));

        // 사원 정보 DTO 변환
        EmployeeDTO employeeDTO = new EmployeeDTO(emp);

        System.out.println("사원 이미지 정보:");
        System.out.println("- 원본 empImg: " + emp.getEmpImg());
        System.out.println("- DTO empImg: " + employeeDTO.getEmpImg());

        // 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();

        // 사원 기본 정보 추가
        response.put("empId", employeeDTO.getEmpId());
        response.put("empName", employeeDTO.getEmpName());
        response.put("empRole", employeeDTO.getEmpRole());
        response.put("empPhone", employeeDTO.getEmpPhone());
        response.put("empAddr", employeeDTO.getEmpAddr());
        response.put("empEmail", employeeDTO.getLoginId());
        response.put("empImg", employeeDTO.getEmpImg());
        response.put("hireDate", employeeDTO.getHireDate());

        // 근태 정보 필드명 수정 - 프론트엔드 컴포넌트와 일치시킴
        if (attendanceInfo != null) {
            // 명시적으로 정수형으로 변환하여 저장
            response.put("attendanceDays", Integer.valueOf(attendanceInfo.getAttendanceDays()));
            response.put("lateCount", Integer.valueOf(attendanceInfo.getLateCount()));
            response.put("absentCount", Integer.valueOf(attendanceInfo.getAbsentCount()));
            response.put("annualLeaveRemain", Integer.valueOf(attendanceInfo.getAnnualLeaveRemain()));
            response.put("annualLeaveTotal", Integer.valueOf(attendanceInfo.getAnnualLeaveTotal()));
            response.put("salaryDay", attendanceInfo.getSalaryDay());
            
            // 디버깅: 변환 후 값과 타입 확인
            System.out.println("변환 후 근무일수: " + response.get("attendanceDays") + 
                               ", 타입: " + response.get("attendanceDays").getClass().getName());
        } else {
            // 기본값 설정
            response.put("attendanceDays", 0);
            response.put("lateCount", 0);
            response.put("absentCount", 0);
            response.put("annualLeaveRemain", 0);
            response.put("annualLeaveTotal", 0);
            response.put("salaryDay", "매월 25일");
        }

        // 부서 정보 추가
        if (emp.getDepartment() != null) {
            response.put("deptId", emp.getDepartment().getDeptId());
            response.put("deptName", emp.getDepartment().getDeptName());
        }

        // 급여 계좌 정보 추가
        String bankName = "";
        switch (employeeDTO.getEmpBank()) {
            case 1: bankName = "국민"; break;
            case 2: bankName = "우리"; break;
            case 3: bankName = "신한"; break;
            case 4: bankName = "하나"; break;
            case 5: bankName = "기업"; break;
            default: bankName = "기업";
        }
        
        response.put("accountInfo", employeeDTO.getEmpAcount() + " " + bankName);

        // 지점 정보 추가
        if (emp.getStore() != null) {
            response.put("storeId", emp.getStore().getStoreId());
            response.put("storeName", emp.getStore().getStoreName());
        }
        
        System.out.println("응답 데이터 준비 완료: " + response);

        return response;
    }
    
    /**
     * 출근 기록 API
     */
    @PostMapping("/attendance/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long empId = Long.valueOf(request.get("empId").toString());
            String checkInTime = (String) request.get("checkInTime");
            boolean isLate = Boolean.valueOf(request.get("isLate").toString());
            
            // 현재 날짜 및 시간 정보
            LocalDateTime now = LocalDateTime.now();
            String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 출근 정보 저장
            attendanceInfoService.recordCheckIn(empId, date, checkInTime, isLate);
            
            // 응답 데이터
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "출근이 기록되었습니다.");
            response.put("checkInTime", checkInTime);
            response.put("date", date);
            response.put("isLate", isLate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "출근 기록 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 퇴근 기록 API
     */
    @PostMapping("/attendance/check-out")
    public ResponseEntity<Map<String, Object>> checkOut(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long empId = Long.valueOf(request.get("empId").toString());
            String checkOutTime = (String) request.get("checkOutTime");
            boolean isEarlyLeave = Boolean.valueOf(request.get("isEarlyLeave").toString());
            
            // 현재 날짜 정보
            LocalDateTime now = LocalDateTime.now();
            String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 정규 퇴근 시간 체크 (18:00으로 가정)
            LocalTime standardOutTime = LocalTime.of(18, 0);
            LocalTime currentTime = LocalTime.parse(checkOutTime, DateTimeFormatter.ofPattern("HH:mm"));
            
            // 정규 퇴근 시간보다 일찍 퇴근하는 경우 응답에 경고 메시지 추가
            Map<String, Object> response = new HashMap<>();
            if (currentTime.isBefore(standardOutTime)) {
                response.put("earlyLeaveWarning", true);
                response.put("warningMessage", "정규 퇴근 시간(18:00) 이전에 퇴근합니다. 계속 진행하시겠습니까?");
                
                // isEarlyLeave가 false이지만 실제로는 조기 퇴근인 경우, 값을 true로 수정
                if (!isEarlyLeave) {
                    isEarlyLeave = true;
                }
            }
            
            // 퇴근 정보 저장
            attendanceInfoService.recordCheckOut(empId, date, checkOutTime, isEarlyLeave);
            
            // 기본 응답 데이터 설정
            response.put("success", true);
            response.put("message", "퇴근이 기록되었습니다.");
            response.put("checkOutTime", checkOutTime);
            response.put("date", date);
            response.put("isEarlyLeave", isEarlyLeave);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "퇴근 기록 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}