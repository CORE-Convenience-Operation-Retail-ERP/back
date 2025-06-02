package com.core.erp.service;

import com.core.erp.domain.DepartmentEntity;
import com.core.erp.domain.EmployeeEntity;
import com.core.erp.domain.StoreEntity;
import com.core.erp.dto.employee.EmployeeListDTO;
import com.core.erp.dto.employee.EmployeeManagementDTO;
import com.core.erp.repository.DepartmentRepository;
import com.core.erp.repository.EmployeeRepository;
import com.core.erp.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmployeeManagementService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final StoreRepository storeRepository;
    private final EmployeeListService employeeListService;
    @Autowired
    private NotificationService notificationService;

    public EmployeeManagementDTO getEmployeeById(Integer empId) {
        // 1. 디버깅 로그 추가
        System.out.println("EmployeeManagementService.getEmployeeById 호출됨: empId = " + empId);
        
        // 먼저 EmployeeListService에서 데이터를 가져와본다
        try {
            EmployeeListDTO listDTO = employeeListService.getEmployeeListDetail(empId.longValue());
            System.out.println("EmployeeListService에서 조회 결과: " + (listDTO != null ? "데이터 있음" : "데이터 없음"));
            
            if (listDTO != null) {
                // 2. 변환 전 EmployeeListDTO 데이터 로깅
                System.out.println("EmployeeListDTO 데이터: " + listDTO.toString());
                
                EmployeeManagementDTO result = convertFromEmployeeListDTO(listDTO);
                // 3. 변환 후 EmployeeManagementDTO 데이터 로깅
                System.out.println("변환된 EmployeeManagementDTO: " + result.toString());
                return result;
            }
        } catch (Exception e) {
            // 예외 로깅
            System.out.println("EmployeeListService 조회 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            // 예외 발생 시 기존 로직으로 진행
        }
        
        // 기존 로직
        EmployeeEntity employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + empId));
        
        System.out.println("직접 Repository에서 조회한 결과: " + employee.toString());
        EmployeeManagementDTO result = convertToDTO(employee);
        System.out.println("최종 변환 결과: " + result.toString());
        return result;
    }

    @Transactional
    public EmployeeManagementDTO createEmployee(EmployeeManagementDTO dto) {
        // ID 중복 체크
        if (dto.getEmpId() != null && employeeRepository.existsById(dto.getEmpId())) {
            throw new RuntimeException("Employee with ID " + dto.getEmpId() + " already exists");
        }

        EmployeeEntity entity = convertToEntity(dto);
        
        // 점주인 경우 매장 정보 처리
        if ("STORE".equals(dto.getEmpRole()) || "점주".equals(dto.getEmpRole())) {
            handleStoreInfo(entity, dto);
        }
        
        EmployeeEntity savedEmployee = employeeRepository.save(entity);
        // 알림 생성 (신규 등록 시)
        try {
            notificationService.createJoinNotification(
                savedEmployee.getEmpId(),
                savedEmployee.getEmpName() + "님이 신규 등록되었습니다.",
                "/headquarters/hr/employees"
            );
        } catch (Exception e) {
            System.err.println("사원 등록 알림 생성 실패: " + e.getMessage());
        }
        return convertToDTO(savedEmployee);
    }

    @Transactional
    public EmployeeManagementDTO updateEmployee(EmployeeManagementDTO dto) {
        EmployeeEntity existingEmployee = employeeRepository.findById(dto.getEmpId())
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + dto.getEmpId()));

        // 기존 엔티티에 DTO 값 업데이트
        updateEmployeeFromDTO(existingEmployee, dto);
        
        // 점주인 경우 매장 정보 처리
        if ("STORE".equals(dto.getEmpRole()) || "점주".equals(dto.getEmpRole())) {
            handleStoreInfo(existingEmployee, dto);
        }
        
        EmployeeEntity updatedEmployee = employeeRepository.save(existingEmployee);
        // 알림 생성 (수정 시) - 본사 직원(부서ID 4~10)만 생성
        Integer deptId = updatedEmployee.getDepartment() != null ? updatedEmployee.getDepartment().getDeptId() : null;
        if (deptId != null && deptId >= 4 && deptId <= 10) {
            try {
                notificationService.createJoinNotification(
                    updatedEmployee.getEmpId(),
                    updatedEmployee.getEmpName() + "님의 정보가 수정되었습니다.",
                    "/headquarters/hr/employees"
                );
            } catch (Exception e) {
                System.err.println("사원 수정 알림 생성 실패: " + e.getMessage());
            }
        }
        return convertToDTO(updatedEmployee);
    }

    private EmployeeEntity convertToEntity(EmployeeManagementDTO dto) {
        EmployeeEntity entity = new EmployeeEntity();
        
        if (dto.getEmpId() != null) {
            entity.setEmpId(dto.getEmpId());
        }
        
        entity.setEmpName(dto.getEmpName());
        entity.setEmpStatus(dto.getEmpStatus());
        entity.setEmpPhone(dto.getEmpPhone());
        entity.setLoginId(dto.getEmpEmail()); // 이메일을 로그인 ID로 사용
        
        // 프로필 이미지 설정
        if (dto.getEmpImg() != null) {
            entity.setEmpImg(dto.getEmpImg());
        }
        
        // 은행/계좌번호 설정
        if (dto.getEmpBank() != null) {
            entity.setEmpBank(dto.getEmpBank());
        } else {
            entity.setEmpBank(0); // 기본값
        }
        
        if (dto.getEmpAcount() != null) {
            entity.setEmpAcount(dto.getEmpAcount());
        } else {
            entity.setEmpAcount("000000000000"); // 기본값
        }
        
        // 주소 설정
        if (dto.getEmpAddr() != null) {
            entity.setEmpAddr(dto.getEmpAddr());
        }
        
        // 내선번호 설정
        if (dto.getEmpExt() != null && !dto.getEmpExt().isEmpty()) {
            try {
                entity.setEmpExt(Integer.parseInt(dto.getEmpExt()));
            } catch (NumberFormatException e) {
                // 숫자로 변환할 수 없는 경우 null로 설정
                entity.setEmpExt(null);
            }
        } else {
            entity.setEmpExt(null);
        }
        
        // 부서 설정
        if (dto.getDeptCode() != null && !dto.getDeptCode().isEmpty()) {
            DepartmentEntity department = departmentRepository.findByDeptName(dto.getDeptCode());
            entity.setDepartment(department);
        }
        
        // 입사일 설정
        if (dto.getHireDate() != null) {
            LocalDateTime hireDateTime = dto.getHireDate().atTime(LocalTime.MIDNIGHT);
            entity.setHireDate(hireDateTime);
        }
        
        // 직원 역할 설정 (본사/점주)
        if (dto.getEmpRole() != null && !dto.getEmpRole().isEmpty()) {
            // 'STORE'는 DB에 '점주'로 저장
            if ("STORE".equals(dto.getEmpRole())) {
                entity.setEmpRole("점주");
                entity.setWorkType(3); // 점주 workType = 3
                
                // 점주인 경우 부서 ID를 항상 3으로 설정
                try {
                    DepartmentEntity storeDepartment = departmentRepository.findById(3)
                            .orElseGet(() -> departmentRepository.findByDeptName("STORE"));
                    entity.setDepartment(storeDepartment);
                    System.out.println("신규 점주 부서 설정: " + (storeDepartment != null ? 
                            storeDepartment.getDeptId() + "-" + storeDepartment.getDeptName() : "null"));
                } catch (Exception e) {
                    System.err.println("점주 부서 설정 오류: " + e.getMessage());
                }
            } else if ("HQ".equals(dto.getEmpRole())) {
                entity.setEmpRole("본사");
            } else {
                entity.setEmpRole(dto.getEmpRole());
            }
        } else {
            entity.setEmpRole("본사"); // 기본값은 본사 직원
        }
        
        // 필수 필드 기본값 설정 (신규 생성 시)
        if (dto.getEmpId() == null) {
            if (entity.getEmpRole() == null) {
                entity.setEmpRole("본사");
            }
            entity.setEmpGender(0); // 기본값
            entity.setEmpBirth(""); // 기본값
            entity.setLoginPwd("defaultPassword"); // 기본 비밀번호
            entity.setWorkType(1); // 기본값
        }
        
        return entity;
    }

    private void updateEmployeeFromDTO(EmployeeEntity entity, EmployeeManagementDTO dto) {
        if (dto.getEmpName() != null) {
            entity.setEmpName(dto.getEmpName());
        }
        
        if (dto.getEmpStatus() != null) {
            entity.setEmpStatus(dto.getEmpStatus());
        }
        
        if (dto.getEmpPhone() != null) {
            entity.setEmpPhone(dto.getEmpPhone());
        }
        
        if (dto.getEmpEmail() != null) {
            entity.setLoginId(dto.getEmpEmail());
        }
        
        // 프로필 이미지 업데이트 추가
        if (dto.getEmpImg() != null) {
            entity.setEmpImg(dto.getEmpImg());
        }
        
        // 은행/계좌번호 업데이트 추가
        if (dto.getEmpBank() != null) {
            entity.setEmpBank(dto.getEmpBank());
        }
        
        if (dto.getEmpAcount() != null) {
            entity.setEmpAcount(dto.getEmpAcount());
        }
        
        // 주소 업데이트
        if (dto.getEmpAddr() != null) {
            entity.setEmpAddr(dto.getEmpAddr());
        }
        
        // 내선번호 업데이트
        if (dto.getEmpExt() != null) {
            try {
                entity.setEmpExt(Integer.parseInt(dto.getEmpExt()));
            } catch (NumberFormatException e) {
                entity.setEmpExt(null);
            }
        }
        
        // 부서 업데이트
        if (dto.getDeptCode() != null && !dto.getDeptCode().isEmpty()) {
            DepartmentEntity department = departmentRepository.findByDeptName(dto.getDeptCode());
            entity.setDepartment(department);
        }
        
        // 입사일 업데이트
        if (dto.getHireDate() != null) {
            LocalDateTime hireDateTime = dto.getHireDate().atTime(LocalTime.MIDNIGHT);
            entity.setHireDate(hireDateTime);
        }
        
        // 직원 역할 업데이트 (본사/점주)
        if (dto.getEmpRole() != null) {
            // 'STORE'는 DB에 '점주'로 저장
            if ("STORE".equals(dto.getEmpRole())) {
                entity.setEmpRole("점주");
                entity.setWorkType(3); // 점주 workType = 3
                
                // 점주인 경우 부서 ID를 항상 3으로 설정
                try {
                    DepartmentEntity storeDepartment = departmentRepository.findById(3)
                            .orElseGet(() -> departmentRepository.findByDeptName("STORE"));
                    entity.setDepartment(storeDepartment);
                    System.out.println("점주 부서 업데이트: " + (storeDepartment != null ? 
                            storeDepartment.getDeptId() + "-" + storeDepartment.getDeptName() : "null"));
                } catch (Exception e) {
                    System.err.println("점주 부서 업데이트 오류: " + e.getMessage());
                }
            } else if ("HQ".equals(dto.getEmpRole())) {
                entity.setEmpRole("본사");
            } else {
                entity.setEmpRole(dto.getEmpRole());
            }
        }
    }

    // 점주 매장 정보 처리 메서드
    private void handleStoreInfo(EmployeeEntity employee, EmployeeManagementDTO dto) {
        StoreEntity store = null;
        
        // 기존 매장 정보가 있는지 확인
        if (employee.getStore() != null) {
            store = employee.getStore();
        } else {
            // 새 매장 생성
            store = new StoreEntity();
            store.setStoreCreatedAt(LocalDateTime.now());
        }
        
        // 매장 정보 업데이트
        if (dto.getStoreName() != null && !dto.getStoreName().isEmpty()) {
            store.setStoreName(dto.getStoreName());
        } else if (store.getStoreName() == null || store.getStoreName().isEmpty()) {
            store.setStoreName(dto.getEmpName() + "의 매장");
        }
        
        if (dto.getStoreAddr() != null && !dto.getStoreAddr().isEmpty()) {
            store.setStoreAddr(dto.getStoreAddr());
        } else if (store.getStoreAddr() == null || store.getStoreAddr().isEmpty()) {
            store.setStoreAddr(dto.getEmpAddr());
        }
        
        if (dto.getStoreTel() != null && !dto.getStoreTel().isEmpty()) {
            store.setStoreTel(dto.getStoreTel());
        } else if (store.getStoreTel() == null || store.getStoreTel().isEmpty()) {
            store.setStoreTel("02-1234-5678");
        }
        
        // 매장 저장 및 직원과 연결
        StoreEntity savedStore = storeRepository.save(store);
        employee.setStore(savedStore);
        
        // 점주(STORE)인 경우 부서 ID를 3으로 설정
        if ("점주".equals(employee.getEmpRole()) || "STORE".equals(dto.getEmpRole())) {
            try {
                // 부서 ID 3(STORE) 조회
                DepartmentEntity storeDepartment = departmentRepository.findById(3)
                        .orElseGet(() -> departmentRepository.findByDeptName("STORE"));
                
                if (storeDepartment != null) {
                    // 부서 설정
                    employee.setDepartment(storeDepartment);
                    employee.setWorkType(3); // 점주 workType = 3
                }
            } catch (Exception e) {
                System.err.println("점주 부서 설정 중 오류 발생: " + e.getMessage());
            }
        }
    }

    private EmployeeManagementDTO convertToDTO(EmployeeEntity entity) {
        EmployeeManagementDTO dto = new EmployeeManagementDTO();
        
        dto.setEmpId(entity.getEmpId());
        dto.setEmpName(entity.getEmpName());
        dto.setEmpStatus(entity.getEmpStatus());
        dto.setEmpPhone(entity.getEmpPhone());
        dto.setEmpEmail(entity.getLoginId());
        
        // 프로필 이미지 설정 추가
        dto.setEmpImg(entity.getEmpImg());
        
        // 은행/계좌번호 설정 추가
        dto.setEmpBank(entity.getEmpBank());
        dto.setEmpAcount(entity.getEmpAcount());
        
        // 주소 설정
        if (entity.getEmpAddr() != null) {
            dto.setEmpAddr(entity.getEmpAddr());
        }
        
        // 내선번호 설정
        if (entity.getEmpExt() != null) {
            dto.setEmpExt(entity.getEmpExt().toString());
        }
        
        // 부서 설정
        if (entity.getDepartment() != null) {
            dto.setDeptCode(entity.getDepartment().getDeptName());
            dto.setDeptName(entity.getDepartment().getDeptName());
        }
        
        // 입사일 설정
        if (entity.getHireDate() != null) {
            dto.setHireDate(entity.getHireDate().toLocalDate());
        }
        
        // 직원 역할 설정
        if (entity.getEmpRole() != null) {
            // DB의 '점주'는 프론트엔드의 'STORE'로 변환
            if ("점주".equals(entity.getEmpRole())) {
                dto.setEmpRole("STORE");
            } else if ("본사".equals(entity.getEmpRole())) {
                dto.setEmpRole("HQ");
            } else {
                dto.setEmpRole(entity.getEmpRole());
            }
        }
        
        // 매장 정보 설정 (점주인 경우)
        if (entity.getStore() != null) {
            // 매장 정보를 DTO에 설정
            dto.setStoreId(entity.getStore().getStoreId());
            dto.setStoreName(entity.getStore().getStoreName());
            dto.setStoreAddr(entity.getStore().getStoreAddr());
            dto.setStoreTel(entity.getStore().getStoreTel());
        }
        
        return dto;
    }
    
    // EmployeeListDTO를 EmployeeManagementDTO로 변환하는 메서드
    private EmployeeManagementDTO convertFromEmployeeListDTO(EmployeeListDTO listDTO) {
        EmployeeManagementDTO dto = new EmployeeManagementDTO();
        
        // 기본 정보 설정
        if (listDTO.getEmpId() != null) {
            dto.setEmpId(listDTO.getEmpId().intValue());
        }
        
        // 4. 수정: null 체크 추가하여 안전하게 데이터 복사
        dto.setEmpName(listDTO.getEmpName() != null ? listDTO.getEmpName() : "");
        dto.setEmpStatus(listDTO.getEmpStatus() != null ? listDTO.getEmpStatus() : "재직");
        dto.setEmpPhone(listDTO.getEmpPhone() != null ? listDTO.getEmpPhone() : "");
        dto.setEmpEmail(listDTO.getEmpEmail() != null ? listDTO.getEmpEmail() : "");
        dto.setEmpExt(listDTO.getEmpExt() != null ? listDTO.getEmpExt() : "");
        
        // 프로필 이미지 설정 추가
        dto.setEmpImg(listDTO.getEmpImg());
        
        // 주소 정보 설정
        dto.setEmpAddr(listDTO.getEmpAddr() != null ? listDTO.getEmpAddr() : "");
        
        // 부서 정보 설정
        // 5. 수정: 부서 코드와 부서명 분리
        String deptName = listDTO.getDeptName();
        if (deptName != null && !deptName.isEmpty()) {
            dto.setDeptCode(deptName); // deptCode는 프론트엔드에서 선택 목록과 일치해야 함
            dto.setDeptName(deptName); // 표시용 부서명
        } else {
            dto.setDeptCode("");
            dto.setDeptName("");
        }
        
        // 입사일 설정 (문자열에서 LocalDate로 변환)
        if (listDTO.getHireDate() != null && !listDTO.getHireDate().isEmpty()) {
            try {
                // 6. 수정: 다양한 날짜 형식 지원
                String hireDateStr = listDTO.getHireDate();
                LocalDate hireDate;
                
                if (hireDateStr.contains("년") && hireDateStr.contains("월")) {
                    // "YYYY년 MM월 DD일" 형식 처리
                    hireDateStr = hireDateStr.replace("년 ", "-")
                                           .replace("월 ", "-")
                                           .replace("일", "");
                    hireDate = LocalDate.parse(hireDateStr, DateTimeFormatter.ofPattern("yyyy-M-d"));
                } else {
                    // 기본 ISO 형식 (yyyy-MM-dd) 가정
                    hireDate = LocalDate.parse(hireDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                
                dto.setHireDate(hireDate);
            } catch (Exception e) {
                System.out.println("입사일 변환 중 오류: " + e.getMessage());
                // 변환 실패 시 현재 날짜 사용
                dto.setHireDate(LocalDate.now());
            }
        }
        
        // 직원 역할 설정
        if (listDTO.getEmpRole() != null) {
            // DB의 '점주'는 프론트엔드의 'STORE'로 변환
            if ("점주".equals(listDTO.getEmpRole())) {
                dto.setEmpRole("STORE");
            } else if ("본사".equals(listDTO.getEmpRole())) {
                dto.setEmpRole("HQ");
            } else {
                dto.setEmpRole(listDTO.getEmpRole());
            }
        } else {
            dto.setEmpRole("HQ");
        }
        
        // 점주 관련 정보 설정
        if ("STORE".equals(dto.getEmpRole())) {
            // 매장 ID 설정 (EmployeeListDTO에서 가져오기)
            if (listDTO.getStoreId() != null) {
                dto.setStoreId(listDTO.getStoreId().intValue());
                System.out.println("매장 ID 설정: " + listDTO.getStoreId());
            }
            
            dto.setStoreName(listDTO.getStoreName() != null ? listDTO.getStoreName() : "");
            dto.setStoreAddr(listDTO.getStoreAddr() != null ? listDTO.getStoreAddr() : "");
            dto.setStoreTel(listDTO.getStoreTel() != null ? listDTO.getStoreTel() : "");
        }
        
        // 7. 수정: 디버깅 로그
        System.out.println("EmployeeListDTO -> EmployeeManagementDTO 변환 완료: " + dto);
        
        return dto;
    }
} 