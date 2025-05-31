package com.core.erp.repository;

import com.core.erp.domain.ChatMessageEntity;
import com.core.erp.domain.ChatRoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    
    // 특정 채팅방의 메시지 목록 조회 (페이징)
    Page<ChatMessageEntity> findByChatRoomOrderBySentAtDesc(ChatRoomEntity chatRoom, Pageable pageable);
    
    // 특정 채팅방의 최근 메시지 조회 (특정 개수만큼)
    List<ChatMessageEntity> findTop50ByChatRoomOrderBySentAtDesc(ChatRoomEntity chatRoom);
    
    // 메시지 검색 (내용으로 검색, 대소문자 구분 없음)
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.chatRoom.roomId = :roomId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.sentAt DESC")
    List<ChatMessageEntity> findByRoomIdAndContentContainingIgnoreCase(@Param("roomId") Long roomId, @Param("searchTerm") String searchTerm);
    
    // 특정 사용자가 읽지 않은 메시지 조회
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.chatRoom.roomId = :roomId AND (m.readBy IS NULL OR m.readBy NOT LIKE CONCAT('%\"', :empId, '\"%')) AND m.sender.empId != :empId")
    List<ChatMessageEntity> findByRoomIdAndNotReadByUser(@Param("roomId") Long roomId, @Param("empId") Integer empId);
} 