package com.core.pos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSettlementRequestDTO {

    private Integer storeId;
    private Integer partTimerId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime shiftStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime shiftEndTime;

    private Integer empId;               // 정산 등록자 (점주), 수동일 경우만 사용
    private Integer isManual;

    private String type;

    public LocalDate getSettlementDate() {
        return shiftStartTime != null ? shiftStartTime.toLocalDate() : null;
    }
}