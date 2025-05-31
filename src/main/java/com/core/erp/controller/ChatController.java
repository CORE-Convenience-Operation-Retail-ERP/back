package com.core.erp.controller;

import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.chat.ChatMessageDTO;
import com.core.erp.dto.chat.ChatRoomDTO;
import com.core.erp.dto.chat.MessageReactionDTO;
import com.core.erp.dto.chat.TypingStatusDTO;
import com.core.erp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 로그인한 사용자의 채팅방 목록 조회
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getRooms(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        List<ChatRoomDTO> rooms = chatService.getUserChatRooms(principal.getEmpId());
        return ResponseEntity.ok(rooms);
    }

    /**
     * 채팅방 생성
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDTO> createRoom(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        
        ChatRoomDTO chatRoomDTO = new ChatRoomDTO();
        chatRoomDTO.setRoomName((String) request.get("roomName"));
        chatRoomDTO.setRoomType((String) request.get("roomType"));
        
        @SuppressWarnings("unchecked")
        List<Integer> memberIds = (List<Integer>) request.get("memberIds");
        
        ChatRoomDTO createdRoom = chatService.createChatRoom(chatRoomDTO, principal.getEmpId(), memberIds);
        return ResponseEntity.ok(createdRoom);
    }

    /**
     * 특정 채팅방 조회
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDTO> getRoom(
            @PathVariable Long roomId,
            Authentication authentication) {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        ChatRoomDTO room = chatService.getChatRoom(roomId, principal.getEmpId());
        return ResponseEntity.ok(room);
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        List<ChatMessageDTO> messages = chatService.getChatMessages(roomId, principal.getEmpId(), page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * 채팅방 나가기
     */
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            Authentication authentication) {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        chatService.leaveRoom(roomId, principal.getEmpId());
        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방에 사용자 초대
     */
    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<Void> inviteToRoom(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        
        @SuppressWarnings("unchecked")
        List<Integer> memberIds = (List<Integer>) request.get("memberIds");
        
        chatService.inviteToRoom(roomId, principal.getEmpId(), memberIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 메시지 읽음 처리
     */
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long roomId,
            Authentication authentication) {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        chatService.markMessagesAsRead(roomId, principal.getEmpId());
        return ResponseEntity.ok().build();
    }

    /**
     * 메시지 검색
     */
    @GetMapping("/rooms/{roomId}/search")
    public ResponseEntity<List<ChatMessageDTO>> searchMessages(
            @PathVariable Long roomId,
            @RequestParam String q,
            Authentication authentication) {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        List<ChatMessageDTO> results = chatService.searchMessages(roomId, q, principal.getEmpId());
        return ResponseEntity.ok(results);
    }

    /**
     * 웹소켓을 통한 메시지 전송
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO messageDTO, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        
        chatService.sendMessage(messageDTO, principal);
    }

    /**
     * 웹소켓을 통한 타이핑 상태 전송
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingStatusDTO typingStatus, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        
        chatService.handleTypingStatus(typingStatus, principal);
    }

    /**
     * 웹소켓을 통한 이모지 반응 처리
     */
    @MessageMapping("/chat.reaction")
    public void handleReaction(@Payload MessageReactionDTO reactionDTO, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        
        chatService.handleMessageReaction(reactionDTO, principal);
    }

    /**
     * 본사 직원 목록 조회
     */
    @GetMapping("/employees")
    public ResponseEntity<List<Map<String, Object>>> getHeadquartersEmployees() {
        return ResponseEntity.ok(chatService.getHeadquartersEmployees());
    }
} 