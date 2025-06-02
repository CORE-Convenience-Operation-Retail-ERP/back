package com.core.erp.controller;

import com.core.erp.domain.VerifiedDeviceEntity;
import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.partTimer.PartTimerDTO;
import com.core.erp.dto.partTimer.PartTimerSearchDTO;
import com.core.erp.dto.partTimer.PhoneRequestDTO;
import com.core.erp.dto.partTimer.VerifyDeviceDTO;
import com.core.erp.repository.PartTimerRepository;
import com.core.erp.repository.VerifiedDeviceRepository;
import com.core.erp.service.CoolSmsService;
import com.core.erp.service.PartTimeService;
import com.core.erp.service.VerifiedDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api")
@RequiredArgsConstructor
@Slf4j
public class PartTimeController {

    private final PartTimeService partTimerService;
    private final CoolSmsService smsService;
    private final PartTimerRepository partTimerRepository;
    private final VerifiedDeviceService verifiedDeviceService;
    private final VerifiedDeviceRepository verifiedDeviceRepository;



    // 현재 로그인한 사용자 정보 추출
    private CustomPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomPrincipal) auth.getPrincipal();
    }

    // (1) 검색 조회
    @GetMapping("/store/parttimer/search")
    public ResponseEntity<List<PartTimerDTO>> searchPartTimers(
            @ModelAttribute PartTimerSearchDTO searchDTO) {

        log.info("searchDTO: {}", searchDTO);
        CustomPrincipal user = getCurrentUser();
        List<PartTimerDTO> list = partTimerService.searchPartTimers(user.getRole(), user.getStoreId(), searchDTO);
        return ResponseEntity.ok(list);
    }

    // (2) 전체 조회
    @GetMapping("/store/parttimer/list")
    public ResponseEntity<Page<PartTimerDTO>> findAllPartTimers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {


        CustomPrincipal user = getCurrentUser();
        Page<PartTimerDTO> list = partTimerService.findAllPartTimers(user.getRole(), user.getStoreId(), page, size);
        return ResponseEntity.ok(list);
    }

    // (3) 단일 조회
    @GetMapping("/store/parttimer/{id}")
    public ResponseEntity<PartTimerDTO> findPartTimerById(@PathVariable("id") Integer partTimerId) {
        CustomPrincipal user = getCurrentUser();
        PartTimerDTO dto = partTimerService.findPartTimerById(user.getRole(), user.getStoreId(), partTimerId);
        return ResponseEntity.ok(dto);
    }

    // (4) 등록 - FormData용
    @PostMapping(
            value = "/store/parttimer",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<String> registerPartTimer(@ModelAttribute PartTimerDTO partTimerDTO) {
        CustomPrincipal user = getCurrentUser();
        partTimerService.registerPartTimer(user.getStoreId(), partTimerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("등록 완료");
    }

    // (5) 수정
    @PutMapping(value = "/store/parttimer/{id}", consumes = "multipart/form-data")
    public ResponseEntity<String> updatePartTimer(
            @PathVariable("id") Integer partTimerId,
            @ModelAttribute PartTimerDTO partTimerDTO) {

        CustomPrincipal user = getCurrentUser();
        partTimerService.updatePartTimer(user.getRole(), user.getStoreId(), partTimerId, partTimerDTO);
        return ResponseEntity.ok("수정 완료");
    }


    // (6) 삭제
    @DeleteMapping("/store/parttimer/{id}")
    public ResponseEntity<String> deletePartTimer(@PathVariable("id") Integer partTimerId) {
        CustomPrincipal user = getCurrentUser();
        partTimerService.deletePartTimer(user.getRole(), user.getStoreId(), partTimerId);
        return ResponseEntity.ok("삭제 완료");
    }

    @GetMapping("/store/parttimer/dropdown")
    public ResponseEntity<List<PartTimerDTO>> getPartTimersForDropdown(
            @AuthenticationPrincipal CustomPrincipal userDetails) {

        Integer storeId = userDetails.getStoreId();
        String role = userDetails.getRole();

        List<PartTimerDTO> list = partTimerService.findAllByStore(storeId, role);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/public/send-code")
    public ResponseEntity<?> sendCode(@RequestBody PhoneRequestDTO dto) {
        smsService.sendVerificationCode(dto.getPhone());
        return ResponseEntity.ok(Map.of("message", "인증번호가 전송되었습니다."));
    }

    @PostMapping("/public/verify-device")
    public ResponseEntity<String> verifyDevice(@RequestBody VerifyDeviceDTO dto) {
        // 1. 인증 코드 확인
        if (!smsService.verify(dto.getPhone(), dto.getCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("인증 실패");
        }

        // 2. 기기 중복 인증 방지
        Optional<VerifiedDeviceEntity> existing = verifiedDeviceRepository.findByDeviceId(dto.getDeviceId());
        if (existing.isPresent() && !existing.get().getPhone().equals(dto.getPhone())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이 기기는 이미 다른 사용자에게 인증되었습니다.");
        }

        // 3. 인증 기록 저장 또는 갱신
        verifiedDeviceService.verifyDevice(dto.getPhone(), dto.getDeviceId(), dto.getDeviceName());

        // 4. 기존 알바 기기 정보 갱신
        partTimerRepository.findByPartPhone(dto.getPhone()).ifPresent(pt -> {
            pt.setDeviceId(dto.getDeviceId());
            pt.setDeviceName(dto.getDeviceName());
            partTimerRepository.save(pt);
        });

        return ResponseEntity.ok("기기 인증 완료");
    }

    @GetMapping("/public/is-verified")
    public ResponseEntity<?> isVerified(@RequestParam String phone, @RequestParam String deviceId) {
        boolean verified = verifiedDeviceService.isDeviceVerified(phone, deviceId);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @GetMapping("/public/verified-device")
    public ResponseEntity<?> getVerifiedDevice(@RequestParam String phone) {
        Optional<VerifiedDeviceEntity> deviceOpt = verifiedDeviceRepository.findByPhone(phone);

        if (deviceOpt.isPresent()) {
            VerifiedDeviceEntity device = deviceOpt.get();
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "deviceId", device.getDeviceId(),
                    "deviceName", device.getDeviceName()
            ));
        } else {
            return ResponseEntity.ok(Map.of("verified", false));
        }
    }

}
