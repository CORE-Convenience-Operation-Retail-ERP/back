package com.core.erp.service;

import com.core.erp.domain.AnnualLeaveEntity;
import com.core.erp.domain.EmployeeEntity;
import com.core.erp.domain.LeaveReqEntity;
import com.core.erp.domain.ApprLogEntity;
import com.core.erp.dto.AnnualLeaveRequestDTO;
import com.core.erp.repository.AnnualLeaveRepository;
import com.core.erp.repository.EmployeeRepository;
import com.core.erp.repository.LeaveReqRepository;
import com.core.erp.repository.ApprLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.ArrayList;

@Service
public class AnnualLeaveService {

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private AnnualLeaveRepository annualLeaveRepository;
    
    @Autowired
    private LeaveReqRepository leaveReqRepository;
    
    @Autowired
    private ApprLogRepository apprLogRepository;
    
    @Autowired
    private AttendanceInfoService attendanceInfoService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private NotificationService notificationService;
    
    private static final Logger logger = Logger.getLogger(AnnualLeaveService.class.getName());

    @Transactional
    public void requestAnnualLeave(AnnualLeaveRequestDTO requestDTO) {
        // 사원 정보 조회
        EmployeeEntity employee = employeeRepository.findById(requestDTO.getEmpId())
            .orElseThrow(() -> new RuntimeException("사원 정보를 찾을 수 없습니다."));

        // 연차 정보 조회
        AnnualLeaveEntity annualLeave = annualLeaveRepository.findTopByEmployee_EmpIdOrderByYearDesc(requestDTO.getEmpId())
            .orElseThrow(() -> new RuntimeException("연차 정보를 찾을 수 없습니다."));

        // 잔여 연차 확인
        if (annualLeave.getRemDays() == null || annualLeave.getRemDays() < requestDTO.getDays()) {
            throw new RuntimeException("잔여 연차가 부족합니다.");
        }

        // 연차 신청 생성
        LeaveReqEntity leaveReq = new LeaveReqEntity();
        leaveReq.setEmployee(employee);
        leaveReq.setStartDate(LocalDate.parse(requestDTO.getStartDate()));
        leaveReq.setEndDate(LocalDate.parse(requestDTO.getEndDate()));
        leaveReq.setDays(requestDTO.getDays());
        leaveReq.setReqReason(requestDTO.getReason());
        leaveReq.setReqStatus(0); // 0: 대기중
        leaveReq.setCreatedAt(LocalDateTime.now());

        // 연차 신청 저장
        leaveReqRepository.save(leaveReq);
        // 알림 생성 (인사팀+MASTER 전체에게)
        try {
            List<EmployeeEntity> targets = new ArrayList<>();
            targets.addAll(employeeRepository.findByDepartment_DeptId(4)); // 인사팀
            List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10); // MASTER
            for (EmployeeEntity master : masters) {
                if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                    targets.add(master);
                }
            }
            for (EmployeeEntity target : targets) {
                notificationService.createLeaveNotification(
                    target.getEmpId(),
                    employee.getEmpName() + "님이 연차를 신청했습니다.",
                    "/headquarters/hr/annual-leave"
                );
            }
        } catch (Exception e) {
            System.err.println("[연차신청] 인사팀+MASTER 알림 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 연차 승인 처리
     * @param reqId 연차 신청 ID
     * @param approverEmpId 승인자 ID
     * @param approveStatus 승인 상태 (1: 승인, 2: 반려)
     * @param note 승인/반려 사유
     * @return 처리 결과
     */
    @Transactional
    public Map<String, Object> approveLeaveRequest(int reqId, int approverEmpId, int approveStatus, String note) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 연차 신청 정보 조회
            LeaveReqEntity leaveReq = leaveReqRepository.findById(reqId)
                .orElseThrow(() -> new RuntimeException("연차 신청 정보를 찾을 수 없습니다."));
            
            // 승인자 정보 조회
            EmployeeEntity approver = employeeRepository.findById(approverEmpId)
                .orElseThrow(() -> new RuntimeException("승인자 정보를 찾을 수 없습니다."));
            
            // 연차 신청 상태 업데이트
            leaveReq.setReqStatus(approveStatus);
            leaveReqRepository.save(leaveReq);
            
            // 승인 로그 생성
            ApprLogEntity apprLog = new ApprLogEntity();
            apprLog.setLeaveReq(leaveReq);
            apprLog.setEmployee(approver);
            apprLog.setApprStatus(approveStatus);
            apprLog.setApprAt(LocalDateTime.now());
            apprLog.setNote(note);
            apprLogRepository.save(apprLog);
            
            // 승인된 경우 연차 차감 처리
            if (approveStatus == 1) { // 1: 승인
                attendanceInfoService.deductAnnualLeave(leaveReq.getEmployee().getEmpId(), leaveReq.getDays());
                result.put("message", "연차가 승인되었습니다.");
                // 알림 생성 (인사팀+MASTER 전체에게)
                try {
                    List<EmployeeEntity> targets = new ArrayList<>();
                    targets.addAll(employeeRepository.findByDepartment_DeptId(4));
                    List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10);
                    for (EmployeeEntity master : masters) {
                        if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                            targets.add(master);
                        }
                    }
                    for (EmployeeEntity target : targets) {
                        notificationService.createLeaveNotification(
                            target.getEmpId(),
                            "연차가 승인되었습니다.",
                            "/headquarters/hr/annual-leave"
                        );
                    }
                    // 신청자(사원)에게도 알림 전송
                    notificationService.createLeaveNotification(
                        leaveReq.getEmployee().getEmpId(),
                        "신청하신 연차가 승인되었습니다.",
                        "/headquarters/hr/annual-leave"
                    );
                } catch (Exception e) {
                    System.err.println("[연차승인] 인사팀+MASTER+신청자 알림 생성 실패: " + e.getMessage());
                }
            } else if (approveStatus == 2) { // 2: 반려
                result.put("message", "연차가 반려되었습니다.");
                // 알림 생성 (인사팀+MASTER 전체에게)
                try {
                    List<EmployeeEntity> targets = new ArrayList<>();
                    targets.addAll(employeeRepository.findByDepartment_DeptId(4));
                    List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10);
                    for (EmployeeEntity master : masters) {
                        if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                            targets.add(master);
                        }
                    }
                    for (EmployeeEntity target : targets) {
                        notificationService.createLeaveNotification(
                            target.getEmpId(),
                            "연차가 반려되었습니다.",
                            "/headquarters/hr/annual-leave"
                        );
                    }
                    // 신청자(사원)에게도 알림 전송
                    notificationService.createLeaveNotification(
                        leaveReq.getEmployee().getEmpId(),
                        "신청하신 연차가 반려되었습니다.",
                        "/headquarters/hr/annual-leave"
                    );
                } catch (Exception e) {
                    System.err.println("[연차반려] 인사팀+MASTER+신청자 알림 생성 실패: " + e.getMessage());
                }
            } else if (approveStatus == 0) { // 0: 대기 상태로 변경
                result.put("message", "연차가 대기 상태로 변경되었습니다.");
            }
            
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 직원의 연차 신청 목록 조회
     * @param empId 직원 ID
     * @return 연차 신청 목록
     */
    public List<LeaveReqEntity> getLeaveRequestsByEmployee(int empId) {
        try {
            // 로깅 추가
            logger.info("직원 ID: " + empId + "에 대한 연차 신청 조회 시도");
            
            // 직원 정보 조회
            Optional<EmployeeEntity> employeeOpt = employeeRepository.findById(empId);
            
            if (!employeeOpt.isPresent()) {
                logger.warning("직원 ID: " + empId + "에 대한 정보를 찾을 수 없습니다.");
                return Collections.emptyList(); // 빈 목록 반환
            }
            
            // 연차 신청 목록 조회
            List<LeaveReqEntity> requests = leaveReqRepository.findByEmployee(employeeOpt.get());
            logger.info("직원 ID: " + empId + "에 대한 연차 신청 " + requests.size() + "건 조회 성공");
            return requests;
        } catch (Exception e) {
            logger.severe("직원 ID: " + empId + "에 대한 연차 신청 조회 실패: " + e.getMessage());
            return Collections.emptyList(); // 예외 발생 시 빈 목록 반환
        }
    }
    
    /**
     * 모든 직원의 연차 신청 목록 조회 (MASTER 권한용)
     * @return 모든 연차 신청 목록
     */
    public List<LeaveReqEntity> getAllLeaveRequests() {
        try {
            logger.info("모든 연차 신청 목록 조회 시도");
            
            // 모든 레코드 조회 및 로깅
            List<LeaveReqEntity> requests = leaveReqRepository.findAll();
            logger.info("데이터베이스에서 조회된 연차 신청 건수: " + requests.size());
            
            // 각 레코드의 ID 로깅
            if (requests.size() > 0) {
                StringBuilder idLog = new StringBuilder("조회된 연차 신청 ID 목록: ");
                for (LeaveReqEntity req : requests) {
                    idLog.append(req.getReqId()).append(", ");
                }
                logger.info(idLog.toString());
            }
            
            // 조회된 모든 항목이 정상적으로 참조 가능한지 확인
            for (LeaveReqEntity req : requests) {
                try {
                    // 직원 정보 참조 테스트 - LazyInitializationException 방지
                    if (req.getEmployee() != null) {
                        logger.info("연차 신청 ID: " + req.getReqId() + ", 직원 이름: " + req.getEmployee().getEmpName());
                    } else {
                        logger.warning("연차 신청 ID: " + req.getReqId() + "의 직원 정보가 null입니다.");
                    }
                } catch (Exception e) {
                    logger.severe("연차 신청 ID: " + req.getReqId() + " 처리 중 오류: " + e.getMessage());
                }
            }
            
            logger.info("모든 연차 신청 " + requests.size() + "건 조회 성공");
            return requests;
        } catch (Exception e) {
            logger.severe("모든 연차 신청 목록 조회 실패: " + e.getMessage());
            // 스택 트레이스 출력
            e.printStackTrace();
            return Collections.emptyList(); // 예외 발생 시 빈 목록 반환
        }
    }

    /**
     * 연차 신청 ID로 연차 신청 정보 조회
     * @param reqId 연차 신청 ID
     * @return 연차 신청 정보
     */
    public LeaveReqEntity getLeaveRequestById(int reqId) {
        try {
            logger.info("연차 신청 ID: " + reqId + " 조회 시도");
            Optional<LeaveReqEntity> leaveReqOpt = leaveReqRepository.findById(reqId);
            
            if (leaveReqOpt.isPresent()) {
                logger.info("연차 신청 ID: " + reqId + " 조회 성공");
                return leaveReqOpt.get();
            } else {
                logger.warning("연차 신청 ID: " + reqId + "에 대한 정보를 찾을 수 없습니다.");
                return null;
            }
        } catch (Exception e) {
            logger.severe("연차 신청 ID: " + reqId + " 조회 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 연차 신청에 대한 코멘트 조회
     * @param reqId 연차 신청 ID
     * @return 코멘트 목록
     */
    public List<Map<String, Object>> getLeaveRequestComments(int reqId) {
        try {
            logger.info("연차 신청 ID: " + reqId + "에 대한 코멘트 조회 시도");
            
            // 연차 신청 정보 조회
            Optional<LeaveReqEntity> leaveReqOpt = leaveReqRepository.findById(reqId);
            if (!leaveReqOpt.isPresent()) {
                logger.warning("연차 신청 ID: " + reqId + "에 대한 정보를 찾을 수 없습니다.");
                return Collections.emptyList();
            }
            
            try {
                // appr_log 테이블에서 직접 조회 (JPA가 아닌 SQL 쿼리 사용)
                String sql = "SELECT al.log_id as logId, al.appr_status as status, al.note as comment, " +
                        "al.appr_at as approvedAt, e.emp_id as approverId, e.emp_name as approverName " +
                        "FROM appr_log al " +
                        "LEFT JOIN employee e ON al.emp_id = e.emp_id " +
                        "WHERE al.req_id = ? " +
                        "ORDER BY al.appr_at DESC";
                
                logger.info("쿼리 실행: " + sql + " (reqId=" + reqId + ")");
                
                List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql, reqId);
                logger.info("연차 신청 ID: " + reqId + "에 대한 코멘트 " + logs.size() + "건 조회 성공");
                
                // 결과가 없으면 빈 배열 반환
                if (logs.isEmpty()) {
                    logger.info("조회된 코멘트가 없습니다.");
                    return logs;
                }
                
                // 디버깅: 첫 번째 코멘트 정보 출력
                if (!logs.isEmpty()) {
                    Map<String, Object> firstLog = logs.get(0);
                    logger.info("첫 번째 코멘트 정보: status=" + firstLog.get("status") + 
                                ", comment=" + firstLog.get("comment") + 
                                ", approverName=" + firstLog.get("approverName"));
                }
                
                // 결과 데이터 가공 (null 처리 및 필드 타입 변환)
                for (Map<String, Object> log : logs) {
                    // 상태 값 처리 (숫자로 통일)
                    if (log.get("status") != null) {
                        try {
                            log.put("status", Integer.parseInt(log.get("status").toString()));
                        } catch (Exception e) {
                            logger.warning("상태 값 변환 오류: " + e.getMessage());
                            // 기본값 설정
                            log.put("status", 0);
                        }
                    } else {
                        log.put("status", 0); // 기본값
                    }
                    
                    // 코멘트 null 처리
                    if (log.get("comment") == null) {
                        log.put("comment", "");
                    }
                    
                    // 작성자 이름 null 처리
                    if (log.get("approverName") == null) {
                        log.put("approverName", "관리자");
                    }
                }
                
                return logs;
                
            } catch (Exception e) {
                logger.severe("SQL 쿼리 실행 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
                
                // 테스트용 더미 데이터 반환 (실제 환경에서는 제거)
                List<Map<String, Object>> dummyLogs = new ArrayList<>();
                
                Map<String, Object> dummyLog = new HashMap<>();
                dummyLog.put("logId", 1);
                dummyLog.put("status", 0);
                dummyLog.put("comment", "테스트 코멘트입니다. SQL 오류 발생으로 인한 임시 데이터.");
                dummyLog.put("approvedAt", new java.util.Date());
                dummyLog.put("approverId", 1);
                dummyLog.put("approverName", "시스템 관리자");
                
                dummyLogs.add(dummyLog);
                
                logger.info("테스트용 더미 데이터 반환 (1건)");
                return dummyLogs;
            }
            
        } catch (Exception e) {
            logger.severe("연차 신청 ID: " + reqId + "에 대한 코멘트 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList(); // 예외 발생 시 빈 목록 반환
        }
    }

    /**
     * 연차 신청 상태 변경 메소드
     * @param reqId 연차 신청 ID
     * @param newStatus 변경할 상태 (0: 대기중, 1: 승인, 2: 반려)
     * @param approverEmpId 승인자 ID
     * @param note 상태 변경 사유/코멘트
     * @return 처리 결과
     */
    @Transactional
    public Map<String, Object> changeLeaveRequestStatus(int reqId, int newStatus, int approverEmpId, String note) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 연차 신청 정보 조회
            LeaveReqEntity leaveReq = leaveReqRepository.findById(reqId)
                .orElseThrow(() -> new RuntimeException("연차 신청 정보를 찾을 수 없습니다."));
            
            // 승인자 정보 조회
            EmployeeEntity approver = employeeRepository.findById(approverEmpId)
                .orElseThrow(() -> new RuntimeException("승인자 정보를 찾을 수 없습니다."));
            
            // 이전 상태
            int previousStatus = leaveReq.getReqStatus();
            
            // 상태가 동일한 경우 코멘트만 추가
            boolean statusChanged = previousStatus != newStatus;
            
            // 연차 신청 상태 업데이트
            if (statusChanged) {
                leaveReq.setReqStatus(newStatus);
                leaveReqRepository.save(leaveReq);
            }
            
            // 승인 로그 생성
            ApprLogEntity apprLog = new ApprLogEntity();
            apprLog.setLeaveReq(leaveReq);
            apprLog.setEmployee(approver);
            apprLog.setApprStatus(newStatus);
            apprLog.setApprAt(LocalDateTime.now());
            apprLog.setNote(note);
            apprLogRepository.save(apprLog);
            
            // 상태 변경에 따른 처리
            if (statusChanged) {
                if (newStatus == 1 && previousStatus != 1) { // 1: 승인으로 변경된 경우
                    attendanceInfoService.deductAnnualLeave(leaveReq.getEmployee().getEmpId(), leaveReq.getDays());
                    result.put("message", "연차가 승인되었습니다.");
                    // 알림 생성 (인사팀+MASTER 전체에게)
                    try {
                        List<EmployeeEntity> targets = new ArrayList<>();
                        targets.addAll(employeeRepository.findByDepartment_DeptId(4));
                        List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10);
                        for (EmployeeEntity master : masters) {
                            if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                                targets.add(master);
                            }
                        }
                        for (EmployeeEntity target : targets) {
                            notificationService.createLeaveNotification(
                                target.getEmpId(),
                                "연차가 승인되었습니다.",
                                "/headquarters/hr/annual-leave"
                            );
                        }
                        // 신청자(사원)에게도 알림 전송
                        notificationService.createLeaveNotification(
                            leaveReq.getEmployee().getEmpId(),
                            "신청하신 연차가 승인되었습니다.",
                            "/headquarters/hr/annual-leave"
                        );
                    } catch (Exception e) {
                        System.err.println("[연차승인] 인사팀+MASTER+신청자 알림 생성 실패: " + e.getMessage());
                    }
                } else if (newStatus == 2 && previousStatus != 2) { // 2: 반려로 변경된 경우
                    result.put("message", "연차가 반려되었습니다.");
                    // 알림 생성 (인사팀+MASTER 전체에게)
                    try {
                        List<EmployeeEntity> targets = new ArrayList<>();
                        targets.addAll(employeeRepository.findByDepartment_DeptId(4));
                        List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10);
                        for (EmployeeEntity master : masters) {
                            if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                                targets.add(master);
                            }
                        }
                        for (EmployeeEntity target : targets) {
                            notificationService.createLeaveNotification(
                                target.getEmpId(),
                                "연차가 반려되었습니다.",
                                "/headquarters/hr/annual-leave"
                            );
                        }
                        // 신청자(사원)에게도 알림 전송
                        notificationService.createLeaveNotification(
                            leaveReq.getEmployee().getEmpId(),
                            "신청하신 연차가 반려되었습니다.",
                            "/headquarters/hr/annual-leave"
                        );
                    } catch (Exception e) {
                        System.err.println("[연차반려] 인사팀+MASTER+신청자 알림 생성 실패: " + e.getMessage());
                    }
                } else if (newStatus == 0 && previousStatus != 0) { // 0: 대기 상태로 변경된 경우
                    result.put("message", "연차가 대기 상태로 변경되었습니다.");
                }
            } else {
                result.put("message", "코멘트가 추가되었습니다.");
            }
            
            result.put("success", true);
        } catch (Exception e) {
            logger.severe("연차 상태 변경 실패: " + e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
} 