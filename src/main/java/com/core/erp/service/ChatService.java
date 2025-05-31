package com.core.erp.service;

import com.core.erp.domain.*;
import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.chat.ChatMessageDTO;
import com.core.erp.dto.chat.ChatRoomDTO;
import com.core.erp.dto.chat.MessageReactionDTO;
import com.core.erp.dto.chat.TypingStatusDTO;
import com.core.erp.repository.ChatMessageRepository;
import com.core.erp.repository.ChatRoomRepository;
import com.core.erp.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 사용자의 모든 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getUserChatRooms(Integer empId) {
        EmployeeEntity employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 본사 직원인지 확인 (depart_id 4~10)
        Integer deptId = employee.getDepartment().getDeptId();
        if (deptId < 4 || deptId > 10) {
            throw new AccessDeniedException("본사 직원만 접근할 수 있습니다.");
        }
        
        List<ChatRoomEntity> rooms = chatRoomRepository.findByMembersContaining(employee);
        return rooms.stream()
                .map(ChatRoomDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 생성
     */
    @Transactional
    public ChatRoomDTO createChatRoom(ChatRoomDTO chatRoomDTO, Integer creatorId, List<Integer> memberIds) {
        EmployeeEntity creator = employeeRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 본사 직원인지 확인
        Integer deptId = creator.getDepartment().getDeptId();
        if (deptId < 4 || deptId > 10) {
            throw new AccessDeniedException("본사 직원만 접근할 수 있습니다.");
        }
        
        // 그룹 또는 개인 채팅방 설정
        ChatRoomEntity.RoomType roomType = 
                memberIds.size() > 1 ? ChatRoomEntity.RoomType.GROUP : ChatRoomEntity.RoomType.INDIVIDUAL;
        
        // 1:1 채팅인 경우 기존 채팅방 확인
        if (roomType == ChatRoomEntity.RoomType.INDIVIDUAL && memberIds.size() == 1) {
            EmployeeEntity otherMember = employeeRepository.findById(memberIds.get(0))
                    .orElseThrow(() -> new RuntimeException("상대방을 찾을 수 없습니다."));
            
            Optional<ChatRoomEntity> existingRoom = 
                    chatRoomRepository.findIndividualRoomByMembers(creator, otherMember, 2L);
            
            if (existingRoom.isPresent()) {
                return ChatRoomDTO.fromEntity(existingRoom.get());
            }
            
            // 1:1 채팅방 이름은 상대방 이름으로 설정
            chatRoomDTO.setRoomName(otherMember.getEmpName());
        }
        
        // 채팅방 엔티티 생성
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .roomName(chatRoomDTO.getRoomName())
                .roomType(roomType)
                .members(new HashSet<>()) // 빈 Set으로 초기화
                .messages(new ArrayList<>()) // 빈 List로 초기화
                .build();
        
        // 채팅방 멤버 추가
        chatRoom.getMembers().add(creator); // 생성자 추가
        
        for (Integer memberId : memberIds) {
            EmployeeEntity member = employeeRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("멤버를 찾을 수 없습니다."));
            
            // 본사 직원인지 확인
            Integer memberDeptId = member.getDepartment().getDeptId();
            if (memberDeptId < 4 || memberDeptId > 10) {
                throw new AccessDeniedException("본사 직원만 채팅방에 추가할 수 있습니다.");
            }
            
            chatRoom.getMembers().add(member);
        }
        
        // 채팅방 저장
        ChatRoomEntity savedRoom = chatRoomRepository.save(chatRoom);
        
        // 입장 메시지 저장
        ChatMessageEntity joinMessage = ChatMessageEntity.builder()
                .chatRoom(savedRoom)
                .sender(creator)
                .content(creator.getEmpName() + "님이 채팅방을 생성했습니다.")
                .messageType(ChatMessageEntity.MessageType.JOIN)
                .build();
        
        chatMessageRepository.save(joinMessage);
        
        // 변경된 데이터로 채팅방 다시 조회
        return ChatRoomDTO.fromEntity(chatRoomRepository.findById(savedRoom.getRoomId())
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다.")));
    }

    /**
     * 채팅방 조회
     */
    @Transactional(readOnly = true)
    public ChatRoomDTO getChatRoom(Long roomId, Integer empId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        EmployeeEntity employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 본사 직원이며 채팅방 멤버인지 확인
        Integer deptId = employee.getDepartment().getDeptId();
        if (deptId < 4 || deptId > 10 || !chatRoom.getMembers().contains(employee)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
        
        return ChatRoomDTO.fromEntity(chatRoom);
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatMessages(Long roomId, Integer empId, int page, int size) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        EmployeeEntity employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 본사 직원이며 채팅방 멤버인지 확인
        Integer deptId = employee.getDepartment().getDeptId();
        if (deptId < 4 || deptId > 10 || !chatRoom.getMembers().contains(employee)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
        
        // 최근 메시지부터 페이징 조회
        return chatMessageRepository.findByChatRoomOrderBySentAtDesc(
                chatRoom, PageRequest.of(page, size, Sort.by("sentAt").descending()))
                .stream()
                .map(ChatMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 전송
     */
    @Transactional
    public ChatMessageDTO sendMessage(ChatMessageDTO messageDTO, CustomPrincipal principal) {
        Long roomId = messageDTO.getRoomId();
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        EmployeeEntity sender = employeeRepository.findById(principal.getEmpId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 본사 직원이며 채팅방 멤버인지 확인
        Integer deptId = sender.getDepartment().getDeptId();
        if (deptId < 4 || deptId > 10 || !chatRoom.getMembers().contains(sender)) {
            throw new AccessDeniedException("메시지를 보낼 권한이 없습니다.");
        }
        
        // 메시지 타입 설정 (기본은 CHAT, 요청에 메시지 타입이 있는 경우 해당 값 사용)
        ChatMessageEntity.MessageType messageType = ChatMessageEntity.MessageType.CHAT;
        if (messageDTO.getMessageType() != null) {
            try {
                messageType = ChatMessageEntity.MessageType.valueOf(messageDTO.getMessageType());
            } catch (IllegalArgumentException e) {
                // 잘못된 메시지 타입은 기본 타입(CHAT)으로 설정
            }
        }
        
        // 메시지 저장
        ChatMessageEntity messageEntity = ChatMessageEntity.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(messageDTO.getContent())
                .messageType(messageType)
                .build();
        
        ChatMessageEntity savedMessage = chatMessageRepository.save(messageEntity);
        ChatMessageDTO savedMessageDTO = ChatMessageDTO.fromEntity(savedMessage);
        
        // 웹소켓으로 메시지 전송 (채팅방)
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, savedMessageDTO);
        
        // 글로벌 채널로도 메시지 전송 (모든 사용자에게 알림 제공)
        messagingTemplate.convertAndSend("/topic/chat/messages", savedMessageDTO);
        
        return savedMessageDTO;
    }
    
    /**
     * 채팅방 나가기
     */
    @Transactional
    public void leaveRoom(Long roomId, Integer empId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        EmployeeEntity employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 본사 직원이며 채팅방 멤버인지 확인
        Integer deptId = employee.getDepartment().getDeptId();
        if (deptId < 4 || deptId > 10) {
            throw new AccessDeniedException("본사 직원만 채팅 기능을 사용할 수 있습니다.");
        }
        
        if (!chatRoom.getMembers().contains(employee)) {
            throw new AccessDeniedException("채팅방의 멤버가 아닙니다.");
        }
        
        // 멤버에서 제거
        chatRoom.getMembers().remove(employee);
        chatRoomRepository.save(chatRoom);
        
        // 나가기 메시지 저장
        ChatMessageEntity leaveMessage = ChatMessageEntity.builder()
                .chatRoom(chatRoom)
                .sender(employee)
                .content(employee.getEmpName() + "님이 퇴장하였습니다.")
                .messageType(ChatMessageEntity.MessageType.LEAVE)
                .build();
        
        chatMessageRepository.save(leaveMessage);
        
        // 웹소켓으로 나가기 메시지 전송
        ChatMessageDTO leaveMessageDTO = ChatMessageDTO.fromEntity(leaveMessage);
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, leaveMessageDTO);
        
        // 채팅방 업데이트 알림
        ChatRoomDTO updatedRoom = ChatRoomDTO.fromEntity(chatRoom);
        messagingTemplate.convertAndSend("/topic/chat/rooms/update", updatedRoom);
    }
    
    /**
     * 채팅방에 사용자 초대
     */
    @Transactional
    public void inviteToRoom(Long roomId, Integer inviterId, List<Integer> memberIds) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        EmployeeEntity inviter = employeeRepository.findById(inviterId)
                .orElseThrow(() -> new RuntimeException("초대자를 찾을 수 없습니다."));
        
        // 본사 직원이며 채팅방 멤버인지 확인
        Integer deptId = inviter.getDepartment().getDeptId();
        if (deptId < 4 || deptId > 10 || !chatRoom.getMembers().contains(inviter)) {
            throw new AccessDeniedException("초대 권한이 없습니다.");
        }
        
        List<EmployeeEntity> newMembers = new ArrayList<>();
        
        for (Integer memberId : memberIds) {
            EmployeeEntity member = employeeRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("초대할 멤버를 찾을 수 없습니다."));
            
            // 본사 직원인지 확인
            Integer memberDeptId = member.getDepartment().getDeptId();
            if (memberDeptId < 4 || memberDeptId > 10) {
                throw new AccessDeniedException("본사 직원만 채팅방에 초대할 수 있습니다.");
            }
            
            // 이미 채팅방 멤버인 경우 건너뜀
            if (chatRoom.getMembers().contains(member)) {
                continue;
            }
            
            chatRoom.getMembers().add(member);
            newMembers.add(member);
        }
        
        // 추가된 멤버가 없으면 처리 종료
        if (newMembers.isEmpty()) {
            return;
        }
        
        // 채팅방 저장
        chatRoomRepository.save(chatRoom);
        
        // 초대 메시지 생성
        StringBuilder inviteMessage = new StringBuilder();
        inviteMessage.append(inviter.getEmpName()).append("님이 ");
        
        if (newMembers.size() == 1) {
            inviteMessage.append(newMembers.get(0).getEmpName());
        } else {
            for (int i = 0; i < newMembers.size(); i++) {
                if (i > 0) {
                    inviteMessage.append(", ");
                }
                inviteMessage.append(newMembers.get(i).getEmpName());
            }
        }
        
        inviteMessage.append("님을 초대했습니다.");
        
        // 초대 메시지 저장
        ChatMessageEntity joinMessage = ChatMessageEntity.builder()
                .chatRoom(chatRoom)
                .sender(inviter)
                .content(inviteMessage.toString())
                .messageType(ChatMessageEntity.MessageType.JOIN)
                .build();
        
        chatMessageRepository.save(joinMessage);
        
        // 웹소켓으로 초대 메시지 전송
        ChatMessageDTO joinMessageDTO = ChatMessageDTO.fromEntity(joinMessage);
        messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, joinMessageDTO);
        
        // 채팅방 업데이트 알림
        ChatRoomDTO updatedRoom = ChatRoomDTO.fromEntity(chatRoom);
        messagingTemplate.convertAndSend("/topic/chat/rooms/update", updatedRoom);
    }

    /**
     * 사용자 목록 조회 (본사 직원만)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHeadquartersEmployees() {
        return employeeRepository.findAll().stream()
                .filter(employee -> {
                    // department가 null인 경우 제외
                    if (employee.getDepartment() == null) {
                        return false;
                    }
                    Integer deptId = employee.getDepartment().getDeptId();
                    return deptId >= 4 && deptId <= 10; // 본사 직원만 필터링
                })
                .map(employee -> {
                    Map<String, Object> employeeMap = new HashMap<>();
                    employeeMap.put("empId", employee.getEmpId());
                    employeeMap.put("empName", employee.getEmpName());
                    employeeMap.put("empRole", employee.getEmpRole());
                    employeeMap.put("deptId", employee.getDepartment().getDeptId());
                    employeeMap.put("deptName", employee.getDepartment().getDeptName());
                    employeeMap.put("empImg", employee.getEmpImg());
                    return employeeMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * 메시지 읽음 처리
     */
    public void markMessagesAsRead(Long roomId, Integer empId) {
        List<ChatMessageEntity> unreadMessages = chatMessageRepository
                .findByRoomIdAndNotReadByUser(roomId, empId);
        
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        for (ChatMessageEntity message : unreadMessages) {
            try {
                Map<String, String> readBy = parseReadBy(message.getReadBy());
                readBy.put(empId.toString(), LocalDateTime.now().toString());
                message.setReadBy(objectMapper.writeValueAsString(readBy));
                
                // 모든 멤버가 읽었는지 확인 (발신자 제외)
                if (readBy.size() >= chatRoom.getMembers().size() - 1) {
                    message.setIsRead(true);
                }
                
                chatMessageRepository.save(message);
                
                // 읽음 상태 업데이트를 웹소켓으로 전송
                ChatMessageDTO messageDTO = ChatMessageDTO.fromEntity(message);
                messagingTemplate.convertAndSend("/topic/chat/room/" + roomId + "/read", messageDTO);
                
            } catch (Exception e) {
                System.err.println("메시지 읽음 처리 오류: " + e.getMessage());
            }
        }
    }

    /**
     * 타이핑 상태 처리
     */
    public void handleTypingStatus(TypingStatusDTO typingStatus, CustomPrincipal principal) {
        typingStatus.setSenderId(principal.getEmpId());
        typingStatus.setSenderName(principal.getEmpName());
        
        // 타이핑 상태를 해당 채팅방의 다른 사용자들에게 전송
        messagingTemplate.convertAndSend(
            "/topic/chat/room/" + typingStatus.getRoomId() + "/typing", 
            typingStatus
        );
    }

    /**
     * 이모지 반응 처리
     */
    public void handleMessageReaction(MessageReactionDTO reactionDTO, CustomPrincipal principal) {
        ChatMessageEntity message = chatMessageRepository.findById(reactionDTO.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
        
        try {
            Map<String, Object> reactions = parseReactions(message.getReactions());
            String emoji = reactionDTO.getEmoji();
            String userId = principal.getEmpId().toString();
            
            @SuppressWarnings("unchecked")
            List<String> userList = (List<String>) reactions.getOrDefault(emoji, new ArrayList<>());
            
            if ("add".equals(reactionDTO.getAction())) {
                if (!userList.contains(userId)) {
                    userList.add(userId);
                }
            } else if ("remove".equals(reactionDTO.getAction())) {
                userList.remove(userId);
            }
            
            if (userList.isEmpty()) {
                reactions.remove(emoji);
            } else {
                reactions.put(emoji, userList);
            }
            
            message.setReactions(objectMapper.writeValueAsString(reactions));
            chatMessageRepository.save(message);
            
            // 반응 업데이트를 웹소켓으로 전송
            reactionDTO.setUserId(principal.getEmpId());
            reactionDTO.setUserName(principal.getEmpName());
            messagingTemplate.convertAndSend(
                "/topic/chat/room/" + message.getChatRoom().getRoomId() + "/reaction", 
                reactionDTO
            );
            
        } catch (Exception e) {
            System.err.println("이모지 반응 처리 오류: " + e.getMessage());
        }
    }

    /**
     * 메시지 검색
     */
    public List<ChatMessageDTO> searchMessages(Long roomId, String searchTerm, Integer empId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        EmployeeEntity employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 사용자가 해당 채팅방의 멤버인지 확인
        if (!chatRoom.getMembers().contains(employee)) {
            throw new IllegalArgumentException("해당 채팅방의 멤버가 아닙니다.");
        }
        
        List<ChatMessageEntity> messages = chatMessageRepository
                .findByRoomIdAndContentContainingIgnoreCase(roomId, searchTerm);
        
        return messages.stream()
                .map(ChatMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 헬퍼 메서드들
    private Map<String, String> parseReadBy(String readByJson) {
        if (readByJson == null || readByJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(readByJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, Object> parseReactions(String reactionsJson) {
        if (reactionsJson == null || reactionsJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(reactionsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
} 