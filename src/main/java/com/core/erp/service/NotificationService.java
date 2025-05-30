package com.core.erp.service;

import com.core.erp.domain.EmployeeEntity;
import com.core.erp.domain.NotificationEntity;
import com.core.erp.dto.notification.NotificationDTO;
import com.core.erp.repository.EmployeeRepository;
import com.core.erp.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 알림 사용자 지정 생성 및 웹소켓을 통한 전송
     */
    @Transactional
    public NotificationDTO createNotification(Integer userId, String type, String content, String link) {
        log.warn("[알림22222-DEBUG] createNotification(userId, type, content, link) 호출됨: userId={}, type={}, content={}, link={}", userId, type, content, link);
        System.out.println("[알림2222222222] createNotification(userId, type, content, link) 호출됨: userId=" + userId + ", type=" + type + ", content=" + content + ", link=" + link);
        log.info("기본 알림 생성 요청: userId={}, type={}, content={}", userId, type, content);
        return createNotification(userId, null, null, type, content, link);
    }
    
    /**
     * 알림 사용자 지정 생성 (이벤트 타입, 대상 부서 지정)
     */
    @Transactional
    public NotificationDTO createNotification(Integer userId, Integer targetDeptId, String eventType, String type, String content, String link) {
        log.warn("[알림-DEBUG222222222] createNotification(userId, targetDeptId, eventType, type, content, link) 호출됨: userId={}, targetDeptId={}, eventType={}, type={}, content={}, link={}", userId, targetDeptId, eventType, type, content, link);
        System.out.println("[알림222222222] createNotification(userId, targetDeptId, eventType, type, content, link) 호출됨: userId=" + userId + ", targetDeptId=" + targetDeptId + ", eventType=" + eventType + ", type=" + type + ", content=" + content + ", link=" + link);
        log.info("[알림 생성] 파라미터: userId={}, targetDeptId={}, eventType={}, type={}, content={}, link={}",
                userId, targetDeptId, eventType, type, content, link);
        try {
            EmployeeEntity user = employeeRepository.findById(userId)
                    .orElse(null);
            if (user == null) {
                log.error("[알림 생성] 사용자 조회 실패: userId={}", userId);
                throw new RuntimeException("사용자를 찾을 수 없습니다.");
            }
            log.info("[알림 생성] 사용자 조회 성공: empId={}, empName={}, departId={}", user.getEmpId(), user.getEmpName(), user.getDepartment() != null ? user.getDepartment().getDeptId() : null);
            Integer deptId = user.getDepartment() != null ? user.getDepartment().getDeptId() : null;
            // eventType별 부서 허용 정책 분기
            if (eventType != null && (eventType.equals("NOTICE") || eventType.equals("STORE_INQUIRY_REPLY"))) {
                // 공지사항, 점주 문의글 답변 알림은 3~10번 부서 허용
                if (deptId == null || deptId < 3 || deptId > 10) {
                    log.error("[알림 생성] 부서 ID 제한으로 알림 생성 실패: deptId={}", deptId);
                    throw new RuntimeException("알림을 받을 수 없는 부서입니다.");
                }
            } else {
                // 그 외 알림은 4~10번 부서만 허용
                if (deptId == null || deptId < 4 || deptId > 10) {
                    log.error("[알림 생성] 부서 ID 제한으로 알림 생성 실패: deptId={}", deptId);
                    throw new RuntimeException("본사 직원만 알림을 받을 수 있습니다.");
                }
            }
            NotificationEntity notification = NotificationEntity.builder()
                    .user(user)
                    .targetDeptId(targetDeptId)
                    .eventType(eventType)
                    .type(type)
                    .content(content)
                    .link(link)
                    .build();
            log.info("[알림 생성] NotificationEntity 생성: {}", notification);
            log.warn("[알림-DEBUG] NotificationEntity 저장 직전: {}", notification);
            NotificationEntity savedNotification = notificationRepository.save(notification);
            log.warn("[알림-DEBUG] NotificationEntity 저장 완료: id={}, 내용={}", savedNotification.getId(), savedNotification.getContent());
            NotificationDTO notificationDTO = NotificationDTO.fromEntity(savedNotification);
            // 웹소켓 전송 로그
            log.info("[알림 생성] 웹소켓 전송 시작: 개인={}, 부서={}, 이벤트={}", userId, targetDeptId, eventType);
            log.info("[알림 생성] [개인] /user/{}/topic/notifications 데이터: {}", userId, notificationDTO);
            
            // 웹소켓 전송 로직 개선
            if (targetDeptId != null) {
                // 부서별 알림 전송
                log.info("[알림 생성] 부서 알림 전송: /topic/notifications/dept/{}", targetDeptId);
                messagingTemplate.convertAndSend("/topic/notifications/dept/" + targetDeptId, notificationDTO);
            }
            
            // 개인 알림 전송 (항상 실행)
            if (userId != null) {
                log.info("[알림 생성] 개인 알림 전송: /user/{}/topic/notifications", userId);
                messagingTemplate.convertAndSendToUser(userId.toString(), "/topic/notifications", notificationDTO);
            }
            
            // 관리자 알림 채널에도 전송 (모든 관리자가 볼 수 있도록)
            if (eventType != null && (eventType.equals("NOTICE") || eventType.equals("HR_JOIN") || 
                eventType.equals("HR_LEAVE") || eventType.equals("PRODUCT_STOCK") || 
                eventType.equals("STORE_INQUIRY"))) {
                log.info("[알림 생성] 관리자 알림 전송: /topic/notifications/admin");
                messagingTemplate.convertAndSend("/topic/notifications/admin", notificationDTO);
            }
            
            log.info("[알림 생성] 웹소켓 전송 완료");
            return notificationDTO;
        } catch (Exception e) {
            log.error("[알림 생성] 예외 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 공지사항 알림 생성 (모든 부서 대상)
     */
    @Transactional
    public NotificationDTO createNoticeNotification(Integer creatorId, String content, String link) {
        return createNotification(creatorId, null, "NOTICE", "INFO", content, link);
    }
    
    /**
     * 인사관리 회원가입 알림 생성
     */
    @Transactional
    public NotificationDTO createJoinNotification(Integer creatorId, String content, String link) {
        return createNotification(creatorId, 4, "HR_JOIN", "INFO", content, link);
    }
    
    /**
     * 인사관리 연차등록 알림 생성
     */
    @Transactional
    public NotificationDTO createLeaveNotification(Integer creatorId, String content, String link) {
        return createNotification(creatorId, 4, "HR_LEAVE", "INFO", content, link);
    }
    
    /**
     * 재고 관련 알림 생성
     */
    @Transactional
    public NotificationDTO createStockNotification(Integer creatorId, String content, String link, String type) {
        return createNotification(creatorId, 6, "PRODUCT_STOCK", type, content, link);
    }
    
    /**
     * 지점 문의 알림 생성
     */
    @Transactional
    public NotificationDTO createStoreInquiryNotification(Integer creatorId, String content, String link) {
        return createNotification(creatorId, 8, "STORE_INQUIRY", "INFO", content, link);
    }

    /**
     * 특정 사용자의 모든 알림 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(Integer userId) {
        EmployeeEntity user = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 읽지 않은 알림 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(Integer userId) {
        EmployeeEntity user = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 부서를 대상으로 한 알림 조회 (개선된 버전)
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsByDeptId(Integer deptId, int page, int size) {
        // Repository에 직접 쿼리 메서드가 있다면 사용, 없다면 기존 방식 유지
        try {
            return notificationRepository.findByTargetDeptIdOrderByCreatedAtDesc(deptId, PageRequest.of(page, size))
                    .stream()
                    .map(NotificationDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Repository 메서드가 없는 경우 기존 방식 사용
            log.warn("Repository 메서드 사용 실패, 기존 방식으로 대체: {}", e.getMessage());
            return notificationRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                    .stream()
                    .filter(notification -> deptId.equals(notification.getTargetDeptId()))
                    .map(NotificationDTO::fromEntity)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 특정 이벤트 타입의 알림 조회 (개선된 버전)
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsByEventType(String eventType, int page, int size) {
        // Repository에 직접 쿼리 메서드가 있다면 사용, 없다면 기존 방식 유지
        try {
            return notificationRepository.findByEventTypeOrderByCreatedAtDesc(eventType, PageRequest.of(page, size))
                    .stream()
                    .map(NotificationDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Repository 메서드가 없는 경우 기존 방식 사용
            log.warn("Repository 메서드 사용 실패, 기존 방식으로 대체: {}", e.getMessage());
            return notificationRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                    .stream()
                    .filter(notification -> eventType.equals(notification.getEventType()))
                    .map(NotificationDTO::fromEntity)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 특정 사용자의 알림 페이지네이션 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(Integer userId, int page, int size) {
        EmployeeEntity user = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return notificationRepository.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Integer userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        // 요청한 사용자가 알림의 소유자인지 확인
        if (notification.getUser().getEmpId() != userId) {
            throw new RuntimeException("다른 사용자의 알림을 읽음 처리할 수 없습니다.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        System.out.println("[알림 읽음 처리] notificationId=" + notificationId + ", read=" + notification.isRead());
    }

    /**
     * 사용자의 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        EmployeeEntity user = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        List<NotificationEntity> unreadNotifications = 
                notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
        
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Integer userId) {
        EmployeeEntity user = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return notificationRepository.countByUserAndReadFalse(user);
    }

    // 전체 알림 반환 (페이징 없이)
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotificationsAll(Integer userId) {
        EmployeeEntity user = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }
} 