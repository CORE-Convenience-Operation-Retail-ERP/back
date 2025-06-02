package com.core.erp.controller;

import com.core.erp.domain.DepartmentEntity;
import com.core.erp.domain.EmployeeEntity;
import com.core.erp.repository.DepartmentRepository;
import com.core.erp.repository.EmployeeRepository;
import com.core.erp.security.JwtTokenProvider;
import com.core.erp.service.EmailService;
import com.core.erp.repository.EmailVerificationRepository;
import com.core.erp.service.NotificationService;
import com.core.erp.service.S3Service;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private S3Service s3Service;
    
    // 파일 업로드 경로 설정 (로컬 백업용)
    private final String uploadDir = "uploads/";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        EmployeeEntity employee = employeeRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 아이디입니다."));
        if (!passwordEncoder.matches(request.getLoginPwd(), employee.getLoginPwd())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        String token = jwtTokenProvider.createToken(employee);
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("empId", employee.getEmpId());
        response.put("deptId", employee.getDepartment().getDeptId());
        response.put("empName", employee.getEmpName());
        response.put("deptName", employee.getDepartment().getDeptName());
        response.put("role", "ROLE_" + jwtTokenProvider.mapDeptIdToRole(employee.getDepartment().getDeptId()));
        
        // 매장 정보가 있는 경우 추가
        if (employee.getStore() != null) {
            response.put("storeId", employee.getStore().getStoreId());
            response.put("storeName", employee.getStore().getStoreName());
        } else {
            response.put("storeId", null);
            response.put("storeName", null);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestPart("data") RegisterRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        // 이미 존재하는 아이디인지 확인
        if (employeeRepository.findByLoginId(request.getLoginId()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 아이디입니다."));
        }
        
        // 이메일 인증 확인
        if (!emailService.isEmailVerified(request.getLoginId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "이메일 인증이 완료되지 않았습니다."));
        }

        try {
            // 회원 정보 저장
            EmployeeEntity employee = new EmployeeEntity();
            employee.setLoginId(request.getLoginId());
            employee.setLoginPwd(passwordEncoder.encode(request.getLoginPwd()));
            employee.setEmpName(request.getName());
            employee.setEmpPhone(request.getPhoneNo());
            
            // 생년월일 및 성별 설정 (주민등록번호 앞 6자리 + 뒤 1자리)
            // 생년월일 형식 변환 (yymmdd -> yyyy-mm-dd)
            String birthDate = request.getBirthDate();
            if (birthDate != null && birthDate.length() == 6) {
                String year = birthDate.substring(0, 2);
                String month = birthDate.substring(2, 4);
                String day = birthDate.substring(4, 6);
                
                // 출생 연도 확인 (성별 번호로 판단)
                String genderCode = request.getGender();
                String fullYear;
                if (genderCode.equals("1") || genderCode.equals("2")) {
                    // 1900년대 출생
                    fullYear = "19" + year;
                } else {
                    // 2000년대 출생
                    fullYear = "20" + year;
                }
                
                // yyyy-mm-dd 형식으로 설정
                String formattedBirthDate = fullYear + "-" + month + "-" + day;
                employee.setEmpBirth(formattedBirthDate);
            } else {
                employee.setEmpBirth("1900-01-01"); // 기본값
            }
            
            // 성별 설정 (1, 3 -> 1(남자), 2, 4 -> 2(여자))
            String genderCode = request.getGender();
            if (genderCode != null) {
                if (genderCode.equals("1") || genderCode.equals("3")) {
                    employee.setEmpGender(1); // 남자
                } else if (genderCode.equals("2") || genderCode.equals("4")) {
                    employee.setEmpGender(2); // 여자
                } else {
                    employee.setEmpGender(0); // 기본값
                }
            } else {
                employee.setEmpGender(0); // 기본값
            }
            
            // 주소 설정
            employee.setEmpAddr(request.getAddress());
            
            // 회원 유형에 따른 설정
            if ("점주".equals(request.getEmpRole())) {
                employee.setEmpRole("점주");
                employee.setWorkType(3); // 점주는 3
                // 부서 설정 (점주는 부서 ID 1)
                DepartmentEntity department = departmentRepository.findById(1).orElse(null);
                if (department == null) {
                    throw new RuntimeException("부서 정보를 찾을 수 없습니다.");
                }
                employee.setDepartment(department);
            } else {
                employee.setEmpRole("본사");
                employee.setWorkType(1); // 본사는 1
                // 부서 설정 (본사는 부서 ID 2)
                DepartmentEntity department = departmentRepository.findById(2).orElse(null);
                if (department == null) {
                    throw new RuntimeException("부서 정보를 찾을 수 없습니다.");
                }
                employee.setDepartment(department);
            }
            
            // 파일 업로드 처리 - S3 사용
            if (file != null && !file.isEmpty()) {
                try {
                    String folder;
                    if ("점주".equals(request.getEmpRole())) {
                        folder = "business-license"; // 사업자등록증
                    } else {
                        folder = "profile"; // 프로필 사진
                    }
                    String imageUrl = s3Service.uploadImage(file, folder);
                    employee.setEmpImg(imageUrl);
                } catch (Exception e) {
                    throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
                }
            }
            
            // 기타 필수 필드 기본값 설정
            employee.setEmpBank(0); // 기본 은행 코드 설정
            employee.setEmpAcount("000000000000"); // 기본 계좌번호 설정
            employee.setEmpStatus("미승인"); // 요구사항에 따라 상태값 "1"로 설정
            employee.setHireDate(LocalDateTime.now()); // hire_date는 null이 될 수 없으므로 현재 날짜로 설정
            employee.setEmailAuth(1); // 이메일 인증 완료 상태
            
            employeeRepository.save(employee);
            // 인사팀(부서ID 4) + MASTER(부서ID 10) 소속 직원 전체에게 알림 전송
            try {
                List<EmployeeEntity> targets = new java.util.ArrayList<>();
                targets.addAll(employeeRepository.findByDepartment_DeptId(4)); // 인사팀
                List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10); // MASTER
                for (EmployeeEntity master : masters) {
                    if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                        targets.add(master);
                    }
                }
                for (EmployeeEntity target : targets) {
                    notificationService.createJoinNotification(
                        target.getEmpId(),
                        employee.getEmpName() + "님이 회원가입하였습니다.",
                        "/headquarters/hr/employees"
                    );
                }
            } catch (Exception e) {
                System.err.println("[회원가입] 인사팀+MASTER 알림 생성 실패: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 회원가입 완료 후 인증 정보 삭제 시도 - 실패해도 회원가입은 성공으로 처리
            try {
                emailVerificationRepository.deleteByEmail(request.getLoginId());
            } catch (Exception ex) {
                // 이메일 인증 정보 삭제 실패해도 회원가입은 계속 진행
                System.err.println("이메일 인증 정보 삭제 실패: " + ex.getMessage());
            }
            
            return ResponseEntity.ok(Map.of("message", "회원가입이 완료되었습니다."));
        } catch (Exception e) {
            System.out.println("[회원가입] 예외 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "회원가입 실패: " + e.getMessage()));
        }
    }
    
    // 이메일 중복 확인 API
    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> request) {
        String loginId = request.get("loginId");
        if (loginId == null || loginId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "available", false,
                "message", "이메일 주소를 입력해주세요."
            ));
        }
        
        // 이메일 형식 검증 (간단한 검증)
        if (!loginId.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of(
                "available", false,
                "message", "유효한 이메일 형식이 아닙니다."
            ));
        }
        
        // 데이터베이스에서 중복 확인
        boolean exists = employeeRepository.findByLoginId(loginId).isPresent();
        
        if (exists) {
            return ResponseEntity.ok(Map.of(
                "available", false,
                "message", "이미 사용 중인 이메일입니다."
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "available", true,
                "message", "사용 가능한 이메일입니다."
            ));
        }
    }

    // 이메일 인증 코드 발송 API
    @PostMapping("/send-verification-email")
    public ResponseEntity<?> sendVerificationEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "유효한 이메일 주소를 입력해주세요."
            ));
        }
        
        try {
            emailService.createAndSendVerificationCode(email);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "인증 이메일이 발송되었습니다. 이메일을 확인해주세요."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "인증 이메일 발송 실패: " + e.getMessage()
            ));
        }
    }

    // 이메일 인증 코드 확인 API
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "이메일과 인증 코드를 모두 입력해주세요."
            ));
        }
        
        boolean verified = emailService.verifyEmail(email, code);
        
        if (verified) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "이메일 인증이 완료되었습니다."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "인증 코드가 일치하지 않거나 만료되었습니다."
            ));
        }
    }

    /**
     * 로컬 업로드 파일 서빙 (기존 파일들을 위한 임시 엔드포인트)
     */
    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Data
    public static class LoginRequest {
        private String loginId;
        private String loginPwd;
    }
    
    @Data
    public static class RegisterRequest {
        private String loginId;
        private String loginPwd;
        private String name;
        private String birthDate;
        private String gender;
        private String phoneNo;
        private String address;
        private String empRole;
    }
}