package com.core.erp.controller;

import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.notification.NotificationDTO;
import com.core.erp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    /**
     * 로그인한 사용자의 모든 알림 조회
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
            
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        List<NotificationDTO> notifications = notificationService.getUserNotifications(principal.getEmpId(), page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림만 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(principal.getEmpId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        long count = notificationService.countUnreadNotifications(principal.getEmpId());
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    /**
     * 특정 부서의 알림 조회
     */
    @GetMapping("/dept/{deptId}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByDept(
            @PathVariable Integer deptId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
            
        List<NotificationDTO> notifications = notificationService.getNotificationsByDeptId(deptId, page, size);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * 특정 이벤트 타입의 알림 조회
     */
    @GetMapping("/event/{eventType}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByEventType(
            @PathVariable String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
            
        List<NotificationDTO> notifications = notificationService.getNotificationsByEventType(eventType, page, size);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * 공지사항 알림 생성 (관리자 권한 필요)
     */
    @PostMapping("/notice")
    public ResponseEntity<NotificationDTO> createNoticeNotification(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {
        log.info("[알림 컨트롤러] /api/notifications/notice 진입, payload={}, principal={}", payload, authentication != null ? authentication.getPrincipal() : null);
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        String content = payload.get("content");
        String link = payload.get("link");
        NotificationDTO notification = notificationService.createNoticeNotification(
                principal.getEmpId(), content, link);
        return ResponseEntity.ok(notification);
    }
    
    /**
     * 인사부서 알림 생성 (회원가입)
     */
    @PostMapping("/hr/join")
    public ResponseEntity<NotificationDTO> createJoinNotification(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {
        log.info("[알림 컨트롤러] /api/notifications/hr/join 진입, payload={}, principal={}", payload, authentication != null ? authentication.getPrincipal() : null);
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        String content = payload.get("content");
        String link = payload.get("link");
        NotificationDTO notification = notificationService.createJoinNotification(
                principal.getEmpId(), content, link);
        return ResponseEntity.ok(notification);
    }
    
    /**
     * 인사부서 알림 생성 (연차)
     */
    @PostMapping("/hr/leave")
    public ResponseEntity<NotificationDTO> createLeaveNotification(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {
        log.info("[알림 컨트롤러] /api/notifications/hr/leave 진입, payload={}, principal={}", payload, authentication != null ? authentication.getPrincipal() : null);
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        String content = payload.get("content");
        String link = payload.get("link");
        NotificationDTO notification = notificationService.createLeaveNotification(
                principal.getEmpId(), content, link);
        return ResponseEntity.ok(notification);
    }
    
    /**
     * 상품팀 재고 알림 생성
     */
    @PostMapping("/product/stock")
    public ResponseEntity<NotificationDTO> createStockNotification(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {
        log.info("[알림 컨트롤러] /api/notifications/product/stock 진입, payload={}, principal={}", payload, authentication != null ? authentication.getPrincipal() : null);
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        String content = payload.get("content");
        String link = payload.get("link");
        String type = payload.get("type"); // INFO, WARNING, ERROR 등
        NotificationDTO notification = notificationService.createStockNotification(
                principal.getEmpId(), content, link, type);
        return ResponseEntity.ok(notification);
    }
    
    /**
     * 지점관리팀 문의 알림 생성
     */
    @PostMapping("/store/inquiry")
    public ResponseEntity<NotificationDTO> createStoreInquiryNotification(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {
        log.info("[알림 컨트롤러] /api/notifications/store/inquiry 진입, payload={}, principal={}", payload, authentication != null ? authentication.getPrincipal() : null);
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        String content = payload.get("content");
        String link = payload.get("link");
        NotificationDTO notification = notificationService.createStoreInquiryNotification(
                principal.getEmpId(), content, link);
        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {
            
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        notificationService.markAsRead(notificationId, principal.getEmpId());
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        notificationService.markAllAsRead(principal.getEmpId());
        return ResponseEntity.ok().build();
    }

    /**
     * 웹소켓을 통한 알림 생성 메시지 처리
     */
    @MessageMapping("/notification.create")
    public void createNotification(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        
        String type = (String) payload.get("type");
        String content = (String) payload.get("content");
        String link = (String) payload.get("link");
        
        notificationService.createNotification(principal.getEmpId(), type, content, link);
    }

    /**
     * 전체 알림(읽은 것 포함, 페이징 없이) 조회
     */
    @GetMapping("/all")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        List<NotificationDTO> notifications = notificationService.getUserNotificationsAll(principal.getEmpId());
        return ResponseEntity.ok(notifications);
    }
} 