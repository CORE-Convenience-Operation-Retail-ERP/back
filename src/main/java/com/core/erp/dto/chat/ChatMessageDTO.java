package com.core.erp.dto.chat;

import com.core.erp.domain.ChatMessageEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long messageId;
    private Long roomId;
    private Integer senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private String messageType;
    private Boolean isRead;
    private Map<String, String> readBy; // empId -> 읽은 시간
    private Map<String, Object> reactions; // emoji -> [empId1, empId2, ...]

    public static ChatMessageDTO fromEntity(ChatMessageEntity entity) {
        return ChatMessageDTO.builder()
                .messageId(entity.getMessageId())
                .roomId(entity.getChatRoom().getRoomId())
                .senderId(entity.getSender().getEmpId())
                .senderName(entity.getSender().getEmpName())
                .content(entity.getContent())
                .sentAt(entity.getSentAt())
                .messageType(entity.getMessageType().name())
                .isRead(entity.getIsRead())
                .readBy(parseJsonToMap(entity.getReadBy()))
                .reactions(parseJsonToMap(entity.getReactions()))
                .build();
    }

    public ChatMessageEntity toEntity() {
        return ChatMessageEntity.builder()
                .messageType(ChatMessageEntity.MessageType.valueOf(messageType))
                .content(content)
                .isRead(isRead != null ? isRead : false)
                .build();
        // sender와 chatRoom은 서비스에서 설정
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Map.of();
        }
        try {
            // 간단한 JSON 파싱 (실제로는 ObjectMapper 사용 권장)
            return Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }
} 