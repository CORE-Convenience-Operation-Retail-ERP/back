package com.core.erp.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionDTO {
    private Long messageId;
    private String emoji;
    private Integer userId;
    private String userName;
    private String action; // "add" or "remove"
} 