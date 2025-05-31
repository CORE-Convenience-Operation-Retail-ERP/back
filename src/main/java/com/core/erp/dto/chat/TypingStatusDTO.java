package com.core.erp.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingStatusDTO {
    private Long roomId;
    private Integer senderId;
    private String senderName;
    private Boolean isTyping;
} 