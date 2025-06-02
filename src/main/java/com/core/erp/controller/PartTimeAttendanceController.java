package com.core.erp.controller;

import com.core.erp.domain.AttendanceEntity;
import com.core.erp.domain.PartTimerEntity;
import com.core.erp.domain.ShiftScheduleEntity;
import com.core.erp.domain.StoreEntity;
import com.core.erp.dto.AttendanceDTO;
import com.core.erp.repository.AttendanceRepository;
import com.core.erp.repository.PartTimerRepository;
import com.core.erp.repository.ShiftScheduleRepository;
import com.core.erp.repository.StoreRepository;
import com.core.erp.service.AttendanceInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PartTimeAttendanceController {

    private final PartTimerRepository partTimerRepository;
    private final StoreRepository storeRepository;
    private final AttendanceRepository attendanceRepository;
    private final ShiftScheduleRepository shiftScheduleRepository;
    private final AttendanceInfoService attendanceService;

    /**
     *  출근 체크 (QR 기반)
     */
    @PostMapping("/public/attendance/part-timer/check-in")
    @Transactional
    public ResponseEntity<?> checkIn(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        try {
            String deviceIdFromQr = (String) payload.get("deviceId");
            String deviceIdFromRequest = request.getHeader("X-DEVICE-ID");

            if (deviceIdFromRequest == null || !deviceIdFromQr.equals(deviceIdFromRequest)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "message", "❌ 다른 기기에서는 출근이 불가능합니다."
                ));
            }

            Integer storeId = (Integer) payload.get("storeId");
            String inTimeStr = (String) payload.get("inTime");
            LocalDateTime inTime = LocalDateTime.parse(inTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            LocalDate today = inTime.toLocalDate();

            PartTimerEntity pt = partTimerRepository.findByDeviceId(deviceIdFromQr)
                    .orElseThrow(() -> new RuntimeException("해당 기기로 등록된 아르바이트가 없습니다."));

            StoreEntity store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("해당 매장을 찾을 수 없습니다."));

            if (attendanceRepository.existsByPartTimerAndAttendDate(pt, today)) {
                throw new RuntimeException("이미 출근 처리가 완료된 상태입니다.");
            }

            ShiftScheduleEntity schedule = shiftScheduleRepository
                    .findByPartTimer_PartTimerIdAndStartTimeBetween(
                            pt.getPartTimerId(),
                            today.atStartOfDay(),
                            today.atTime(23, 59, 59)
                    ).orElse(null);

            int status = 0; // 기본값: 정상 출근
            if (schedule != null && inTime.isAfter(schedule.getStartTime())) {
                status = 1; // 지각
            }

            // 출근 기록 저장
            AttendanceEntity attend = new AttendanceEntity();
            attend.setPartTimer(pt);
            attend.setStore(store);
            attend.setWorkDate(inTime);
            attend.setAttendDate(today);
            attend.setInTime(inTime);
            attend.setAttendStatus(status);
            attendanceRepository.save(attend);


            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "출근 완료",
                    "status", (schedule == null ? "스케줄 없음 (정상 출근)" : (status == 0 ? "정상 출근" : "지각")),
                    "isCheckedInToday", true,
                    "partTimerId", pt.getPartTimerId()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     *  퇴근 체크 (QR 기반)
     */
    @PostMapping("/public/attendance/part-timer/check-out")
    @Transactional
    public ResponseEntity<?> checkOut(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        try {
            String deviceIdFromQr = (String) payload.get("deviceId");
            String deviceIdFromRequest = request.getHeader("X-DEVICE-ID");

            if (deviceIdFromRequest == null || !deviceIdFromQr.equals(deviceIdFromRequest)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "message", "❌ 다른 기기에서는 퇴근이 불가능합니다."
                ));
            }

            Integer storeId = (Integer) payload.get("storeId");
            String outTimeStr = (String) payload.get("outTime");
            LocalDateTime outTime = LocalDateTime.parse(outTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            LocalDate today = outTime.toLocalDate();

            PartTimerEntity pt = partTimerRepository.findByDeviceId(deviceIdFromQr)
                    .orElseThrow(() -> new RuntimeException("해당 기기로 등록된 아르바이트가 없습니다."));

            AttendanceEntity attend = attendanceRepository
                    .findTopByPartTimerAndAttendDateOrderByInTimeAsc(pt, today)
                    .orElseThrow(() -> new RuntimeException("출근 기록이 없습니다. 퇴근할 수 없습니다."));

            if (attend.getOutTime() != null) {
                throw new RuntimeException("이미 퇴근 처리되었습니다.");
            }

            attend.setOutTime(outTime);
            attendanceRepository.save(attend);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "퇴근 완료",
                    "outTime", outTime.toString(),
                    "isCheckedInToday", false,
                    "partTimerId", pt.getPartTimerId()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @GetMapping("/attendance/part-timer/list")
    public ResponseEntity<?> getAttendanceList(
            @RequestParam Integer storeId,
            @RequestParam(required = false) Integer partTimerId,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<AttendanceEntity> result = attendanceService.getPartTimerAttendanceList(
                storeId, partTimerId, partName, position, startDate, endDate, page, size
        );

        // Entity → DTO 변환
        Page<AttendanceDTO> dtoPage = result.map(AttendanceDTO::new);

        return ResponseEntity.ok(dtoPage);
    }

}
