package com.core.erp.service;

import com.core.erp.domain.AnnualLeaveEntity;
import com.core.erp.domain.AttendanceEntity;
import com.core.erp.domain.EmployeeEntity;
import com.core.erp.domain.SalaryEntity;
import com.core.erp.dto.AnnualLeaveDTO;
import com.core.erp.dto.AttendanceDTO;
import com.core.erp.dto.AttendanceInfoDTO;
import com.core.erp.dto.employee.EmployeeDTO;
import com.core.erp.dto.salary.SalaryDTO;
import com.core.erp.repository.AnnualLeaveRepository;
import com.core.erp.repository.AttendanceRepository;
import com.core.erp.repository.EmployeeRepository;
import com.core.erp.repository.SalaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceInfoService {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AnnualLeaveRepository annualLeaveRepository;
    @Autowired
    private SalaryRepository salaryRepository;
    
    // 매장 직원이나 점주에 해당하는 역할 코드 목록
    private static final List<String> STORE_ROLES = Arrays.asList("STORE", "점주", "MANAGER", "STAFF");

    public AttendanceInfoDTO getEmployeeAttendanceInfo(int empId) {
        EmployeeEntity empEntity = employeeRepository.findById(empId)
            .orElseThrow(() -> new RuntimeException("사원 정보 없음"));
        EmployeeDTO emp = new EmployeeDTO(empEntity);

        // 입사일부터 오늘까지의 총 근무 가능 일수 계산 (주말 제외)
        int totalWorkingDays = 0;
        LocalDate hireDate = null;
        
        if (empEntity.getHireDate() != null) {
            hireDate = empEntity.getHireDate().toLocalDate();
            LocalDate today = LocalDate.now();
            
            // 입사일부터 오늘까지 날짜를 순회하면서 주말 제외한 근무일수 계산
            for (LocalDate date = hireDate; !date.isAfter(today); date = date.plusDays(1)) {
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                    totalWorkingDays++;
                }
            }
            
            System.out.println("입사일(" + hireDate + ")부터 오늘까지 총 근무일수(주말 제외): " + totalWorkingDays);
        } else {
            System.out.println("입사일이 null입니다. 사원ID: " + empId);
            // 입사일이 없는 경우 기본값으로 6개월(약 130일)의 근무일 가정
            totalWorkingDays = 130;
        }

        // 실제 출근-퇴근 완료된 날짜 수 계산
        List<AttendanceEntity> attendanceEntities = attendanceRepository.findByEmployee_EmpId(empId);
        System.out.println("사원ID: " + empId + ", 총 출퇴근 기록 수: " + attendanceEntities.size());
        
        // 디버깅: 모든 출퇴근 기록 출력
        for (AttendanceEntity attend : attendanceEntities) {
            System.out.println("기록ID: " + attend.getAttendId() 
                + ", 날짜: " + attend.getAttendDate() 
                + ", 출근시간: " + attend.getInTime() 
                + ", 퇴근시간: " + attend.getOutTime() 
                + ", 상태: " + attend.getAttendStatus());
        }
        
        int actualAttendanceDays = (int) attendanceEntities.stream()
            .filter(a -> a.getInTime() != null && a.getOutTime() != null)
            .map(a -> a.getAttendDate())
            .distinct()
            .count();
        
        System.out.println("사원ID: " + empId + ", 출퇴근 완료된 실제 근무일수: " + actualAttendanceDays);
        
        // 출퇴근 기록이 없거나 적을 경우 입사일 기준 근무일수 사용
        if (actualAttendanceDays < 5 && totalWorkingDays > 0) {
            System.out.println("출퇴근 기록이 적어 입사일 기준 근무일수를 사용합니다: " + totalWorkingDays);
            actualAttendanceDays = totalWorkingDays;
        }

        List<AttendanceDTO> attendances = attendanceEntities.stream().map(AttendanceDTO::new).collect(Collectors.toList());
        int lateCount = (int) attendances.stream().filter(a -> a.getAttendStatus() == 2).count();
        int absentCount = (int) attendances.stream().filter(a -> a.getAttendStatus() == 3).count();

        Optional<AnnualLeaveEntity> leaveEntityOpt = annualLeaveRepository.findTopByEmployee_EmpIdOrderByYearDesc(empId);
        AnnualLeaveDTO leave = leaveEntityOpt.map(AnnualLeaveDTO::new).orElse(null);
        int annualLeaveUsed = leave != null ? leave.getUsedDays() : 0;
        int annualLeaveTotal = leave != null ? leave.getTotalDays() : 0;
        int annualLeaveRemain = leave != null ? leave.getRemDays() : 0;

        Optional<SalaryEntity> salaryEntityOpt = salaryRepository.findTopByEmployee_EmpIdOrderByPayDateDesc(empId);
        SalaryDTO salary = salaryEntityOpt.map(SalaryDTO::new).orElse(null);
        String salaryDay = (salary != null && salary.getPayDate() != null) ? salary.getPayDate().toString() : "매월 5일";

        AttendanceInfoDTO attendInfoDto = new AttendanceInfoDTO();
        attendInfoDto.setEmployee(emp);
        // 마이페이지에는 실제 출퇴근 완료 일수를 표시
        attendInfoDto.setAttendanceDays(actualAttendanceDays);
        attendInfoDto.setLateCount(lateCount);
        attendInfoDto.setAbsentCount(absentCount);
        attendInfoDto.setAnnualLeaveUsed(annualLeaveUsed);
        attendInfoDto.setAnnualLeaveTotal(annualLeaveTotal);
        attendInfoDto.setAnnualLeaveRemain(annualLeaveRemain);
        attendInfoDto.setSalaryDay(salaryDay);

        return attendInfoDto;
    }
    
    /**
     * 직원이 본사 직원인지 확인
     * STORE, 점주 역할을 가진 직원이 아닌 경우 본사 직원으로 간주
     */
    private boolean isHeadquartersEmployee(EmployeeEntity employee) {
        // 역할 코드가 없는 경우 기본값 설정
        String empRole = employee.getEmpRole() != null ? employee.getEmpRole() : "";
        
        // 매장 직원/점주 역할을 가진 경우 본사 직원이 아님
        for (String storeRole : STORE_ROLES) {
            if (empRole.contains(storeRole)) {
                return false;
            }
        }
        
        // 그 외 모든 경우는 본사 직원으로 간주
        return true;
    }
    
    /**
     * 출근 기록 저장 - 누적 방식으로 새 레코드 생성
     */
    @Transactional
    public void recordCheckIn(Long empId, String date, String checkInTime, boolean isLate) {
        EmployeeEntity employee = employeeRepository.findById(empId.intValue())
            .orElseThrow(() -> new RuntimeException("사원 정보 없음"));
        
        // 본사 직원인지 확인
        if (!isHeadquartersEmployee(employee)) {
            return; // 본사 직원이 아니면 처리하지 않음
        }
        
        LocalDate today = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime now = LocalDateTime.now();
            
        // 출근 시간 파싱 (예: "09:30" 형식)
            String[] timeParts = checkInTime.split(":");
            LocalDateTime inTimeValue = now.withHour(Integer.parseInt(timeParts[0]))
                .withMinute(Integer.parseInt(timeParts[1]))
                .withSecond(0)
                .withNano(0);
            
        // 항상 새 레코드 생성 (누적 방식)
        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setEmployee(employee);
        attendance.setAttendDate(today);
        attendance.setWorkDate(now);
        attendance.setInTime(inTimeValue);
            attendance.setAttendStatus(isLate ? 2 : 1); // 1: 정상출근, 2: 지각
            attendanceRepository.save(attendance);
    }
    
    /**
     * 퇴근 기록 저장 - 오늘의 최신 출근 기록에 퇴근 시간 추가
     */
    @Transactional
    public void recordCheckOut(Long empId, String date, String checkOutTime, boolean isEarlyLeave) {
        EmployeeEntity employee = employeeRepository.findById(empId.intValue())
            .orElseThrow(() -> new RuntimeException("사원 정보 없음"));
        
        // 본사 직원인지 확인
        if (!isHeadquartersEmployee(employee)) {
            return; // 본사 직원이 아니면 처리하지 않음
        }
        
        LocalDate today = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime now = LocalDateTime.now();
            
            // 퇴근 시간 파싱 (예: "18:30" 형식)
            String[] timeParts = checkOutTime.split(":");
            LocalDateTime outTimeValue = now.withHour(Integer.parseInt(timeParts[0]))
                .withMinute(Integer.parseInt(timeParts[1]))
                .withSecond(0)
                .withNano(0);
            
        // 오늘 최신 출근 기록 조회
        Optional<AttendanceEntity> latestCheckIn = attendanceRepository.findTopByEmployee_EmpIdAndAttendDateOrderByInTimeDesc(
            empId.intValue(), today);
        
        if (latestCheckIn.isPresent()) {
            // 최신 출근 기록에 퇴근 시간 추가
            AttendanceEntity attendance = latestCheckIn.get();
            attendance.setOutTime(outTimeValue);
            
            // 조퇴인 경우 상태 업데이트 (이미 지각인 경우 상태 유지)
            if (isEarlyLeave && attendance.getAttendStatus() == 1) {
                attendance.setAttendStatus(4); // 4: 조퇴
            }
            
            attendanceRepository.save(attendance);
        } else {
            // 출근 기록이 없는 경우, 새로운 레코드 생성
            AttendanceEntity attendance = new AttendanceEntity();
            attendance.setEmployee(employee);
            attendance.setAttendDate(today);
            attendance.setWorkDate(now);
            // 기본 출근 시간을 09:00으로 설정
            attendance.setInTime(now.withHour(9).withMinute(0).withSecond(0).withNano(0));
            attendance.setOutTime(outTimeValue);
            attendance.setAttendStatus(isEarlyLeave ? 4 : 1);
            attendanceRepository.save(attendance);
        }
    }
    
    /**
     * 연차 승인 시 연차 차감 처리
     * @param empId 직원 ID
     * @param days 차감할 연차 일수
     * @return 처리 결과
     */
    @Transactional
    public boolean deductAnnualLeave(int empId, int days) {
        Optional<AnnualLeaveEntity> leaveOpt = annualLeaveRepository.findTopByEmployee_EmpIdOrderByYearDesc(empId);
        if (leaveOpt.isEmpty()) {
            return false;
        }
        AnnualLeaveEntity annualLeave = leaveOpt.get();
        if (annualLeave.getRemDays() == null || annualLeave.getRemDays() < days) {
            return false;
        }
        annualLeave.setUsedDays(annualLeave.getUsedDays() + days);
        annualLeave.setRemDays(annualLeave.getTotalDays() - annualLeave.getUsedDays());
        annualLeave.setUadateAt(LocalDateTime.now());
        annualLeaveRepository.save(annualLeave);
        return true;
    }

    // PartTimer 출퇴근 조회
    public Page<AttendanceEntity> getPartTimerAttendanceList(
            Integer storeId,
            Integer partTimerId,
            String partName,
            String position,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "inTime"));

        return attendanceRepository.findAttendancesWithFilter(
                storeId,
                partTimerId,
                partName,
                position,
                startDate != null ? startDate.atStartOfDay() : null,
                endDate != null ? endDate.atTime(23, 59, 59) : null,
                pageable
        );
    }

} 