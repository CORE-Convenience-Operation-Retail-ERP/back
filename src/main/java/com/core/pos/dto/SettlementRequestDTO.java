package com.core.pos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRequestDTO {

    private Integer storeId;           // 매장 ID (필수)
    private Integer empId;             // 정산 점주 ID (수동 정산 시 필요)
    private Integer partTimerId;       // 정산 대상 아르바이트 ID (교대 정산 시 필요)

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate targetDate;      // 정산 기준일 (일별, 월별, 연별 공통)
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate startDate;       // 정산 시작일 (월별/연별)
    
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate endDate;         // 정산 종료일 (월별/연별)

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime shiftStartTime; // 교대 시작 시간 (교대 정산)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime shiftEndTime;   // 교대 종료 시간 (교대 정산)

    private Integer isManual;           // 수동 정산 여부 (0: 자동, 1: 수동)

    private String type;
}
