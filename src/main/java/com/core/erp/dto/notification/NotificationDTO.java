package com.core.erp.dto.notification;

import com.core.erp.domain.NotificationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Integer userId;
    private String userName;
    private Integer targetDeptId;
    private String eventType;
    private String type;
    private String content;
    private String link;
    private LocalDateTime createdAt;
    private boolean read;
    
    // NotificationEntity를 NotificationDTO로 변환하는 정적 메서드
    public static NotificationDTO fromEntity(NotificationEntity entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getEmpId())
                .userName(entity.getUser().getEmpName())
                .targetDeptId(entity.getTargetDeptId())
                .eventType(entity.getEventType())
                .type(entity.getType())
                .content(entity.getContent())
                .link(entity.getLink())
                .createdAt(entity.getCreatedAt())
                .read(entity.isRead())
                .build();
    }
} 