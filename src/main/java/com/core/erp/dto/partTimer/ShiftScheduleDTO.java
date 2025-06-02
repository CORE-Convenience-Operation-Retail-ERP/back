package com.core.erp.dto.partTimer;

import com.core.erp.domain.ShiftScheduleEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ShiftScheduleDTO {

    private int scheduleId;
    private Integer partTimerId; // FK (id만 관리)
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String bgColor;
    private String partImg;


    // Entity → DTO 변환 생성자
    public ShiftScheduleDTO(ShiftScheduleEntity entity) {
        this.scheduleId = entity.getScheduleId();
        this.partTimerId = entity.getPartTimer() != null ? entity.getPartTimer().getPartTimerId() : null;
        this.title = entity.getTitle();
        this.startTime = entity.getStartTime();
        this.endTime = entity.getEndTime();
        this.bgColor = entity.getBgColor();
        this.partImg = entity.getPartTimer() != null ? entity.getPartTimer().getPartImg() : null;
    }
}