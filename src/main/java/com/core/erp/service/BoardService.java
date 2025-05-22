// CORE-ERP-POS-Backend/src/main/java/com/core/erp/service/BoardService.java
package com.core.erp.service;

import com.core.erp.domain.EmployeeEntity;
import com.core.erp.domain.TblBoardCommentsEntity;
import com.core.erp.domain.TblBoardPostsEntity;
import com.core.erp.dto.BoardCommentResponseDTO;
import com.core.erp.dto.BoardPostResponseDTO;
import com.core.erp.dto.TblBoardCommentsDTO;
import com.core.erp.dto.TblBoardPostsDTO;
import com.core.erp.repository.EmployeeRepository;
import com.core.erp.repository.TblBoardCommentsRepository;
import com.core.erp.repository.TblBoardPostsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final TblBoardPostsRepository boardPostsRepository;
    private final TblBoardCommentsRepository boardCommentsRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    // 게시판 타입별 게시글 목록 조회
    public List<BoardPostResponseDTO> getBoardPostsByType(int boardType) {
        List<Object[]> postsWithCommentStatus = boardPostsRepository.findByBoardTypeWithCommentStatus(boardType);
        List<BoardPostResponseDTO> result = new ArrayList<>();
        
        for (Object[] obj : postsWithCommentStatus) {
            TblBoardPostsEntity post = (TblBoardPostsEntity) obj[0];
            boolean hasComment = (boolean) obj[1];
            
            BoardPostResponseDTO dto = new BoardPostResponseDTO(post);
            dto.setHasComment(hasComment);
            
            if (hasComment) {
                List<TblBoardCommentsEntity> comments = boardCommentsRepository.findByPostOrderByComCreatedAtDesc(post);
                dto.setComments(comments.stream()
                    .map(comment -> {
                        BoardCommentResponseDTO commentDto = new BoardCommentResponseDTO(comment);
                        // 작성자 정보 설정
                        EmployeeEntity commentEmployee = employeeRepository.findById(post.getEmployee().getEmpId())
                            .orElse(null);
                        if (commentEmployee != null) {
                            commentDto.setEmpName(commentEmployee.getEmpName());
                        }
                        return commentDto;
                    })
                    .collect(Collectors.toList()));
            } else {
                dto.setComments(new ArrayList<>());
            }
            
            result.add(dto);
        }
        
        return result;
    }
    
    // 게시글 단일 조회
    public BoardPostResponseDTO getBoardPost(int postId) {
        TblBoardPostsEntity post = boardPostsRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        
        BoardPostResponseDTO dto = new BoardPostResponseDTO(post);
        boolean hasComment = boardCommentsRepository.existsByPost(post);
        dto.setHasComment(hasComment);
        
        if (hasComment) {
            List<TblBoardCommentsEntity> comments = boardCommentsRepository.findByPostOrderByComCreatedAtDesc(post);
            dto.setComments(comments.stream()
                .map(comment -> {
                    BoardCommentResponseDTO commentDto = new BoardCommentResponseDTO(comment);
                    // 작성자 정보 설정
                    EmployeeEntity commentEmployee = employeeRepository.findById(post.getEmployee().getEmpId())
                        .orElse(null);
                    if (commentEmployee != null) {
                        commentDto.setEmpName(commentEmployee.getEmpName());
                    }
                    return commentDto;
                })
                .collect(Collectors.toList()));
        } else {
            dto.setComments(new ArrayList<>());
        }
        
        return dto;
    }
    
    // 최근 게시글 조회 (위젯용)
    public List<BoardPostResponseDTO> getRecentPosts(int limit) {
        List<TblBoardPostsEntity> recentPosts = boardPostsRepository.findTop4ByOrderByBoardCreatedAtDesc();
        
        List<BoardPostResponseDTO> result = new ArrayList<>();
        for (TblBoardPostsEntity post : recentPosts) {
            BoardPostResponseDTO dto = new BoardPostResponseDTO(post);
            boolean hasComment = boardCommentsRepository.existsByPost(post);
            dto.setHasComment(hasComment);
            
            // 댓글 정보는 위젯에서 표시하지 않으므로 빈 리스트로 설정
            dto.setComments(new ArrayList<>());
            result.add(dto);
        }
        
        return result;
    }
    
    // 게시글 등록
    @Transactional
    public BoardPostResponseDTO createBoardPost(TblBoardPostsDTO dto, String loginId) {
        EmployeeEntity employee = employeeRepository.findByLoginId(loginId)
            .orElseThrow(() -> new RuntimeException("사원 정보를 찾을 수 없습니다."));
        TblBoardPostsEntity entity = new TblBoardPostsEntity(dto);
        entity.setEmployee(employee);
        entity.setBoardCreatedAt(LocalDateTime.now());
        TblBoardPostsEntity savedEntity = boardPostsRepository.save(entity);
        // 알림 생성 코드 추가
        System.out.println("[알림] 게시글 등록 알림 코드 진입");
        try {
            String link = "/headquarters/board/notice"; // 기본값: 공지사항
            String postTypeLabel = "게시글";
            if (Objects.equals(dto.getBoardType(), 1)) { // 공지사항
                link = "/headquarters/board/notice";
                postTypeLabel = "공지사항";
            } else if (Objects.equals(dto.getBoardType(), 2)) { // 건의사항
                link = "/headquarters/board/suggestions";
                postTypeLabel = "건의사항";
            } else if (Objects.equals(dto.getBoardType(), 3)) { // 점포 문의 사항
                link = "/headquarters/board/store-inquiries";
                postTypeLabel = "점포 문의 사항";
            }
            String contentMsg = String.format("[게시판] %s 글이 등록되었습니다.", postTypeLabel);
            List<EmployeeEntity> targets = new ArrayList<>();
            if (Objects.equals(dto.getBoardType(), 1)) {
                // 공지사항: 본사 직원 전체(부서ID 4~10)
                for (int deptId = 3; deptId <= 10; deptId++) {
                    targets.addAll(employeeRepository.findByDepartment_DeptId(deptId));
                }
            } else {
                // 2,3: 지점관리+MASTER
                targets.addAll(employeeRepository.findByDepartment_DeptId(8));
                List<EmployeeEntity> masters = employeeRepository.findByDepartment_DeptId(10);
                for (EmployeeEntity master : masters) {
                    if (targets.stream().noneMatch(e -> e.getEmpId() == master.getEmpId())) {
                        targets.add(master);
                    }
                }
            }
            for (EmployeeEntity target : targets) {
                notificationService.createNotification(
                    target.getEmpId(),
                    Objects.equals(dto.getBoardType(), 1) ? null : 8,
                    "BOARD_POST",
                    "INFO",
                    contentMsg,
                    link
                );
            }
        } catch (Exception e) {
            System.out.println("[알림] 게시글 등록 알림 예외: " + e.getMessage());
            e.printStackTrace();
        }
        return new BoardPostResponseDTO(savedEntity);
    }
    
    // 게시글 수정
    @Transactional
    public BoardPostResponseDTO updateBoardPost(int postId, TblBoardPostsDTO dto, String loginId) {
        TblBoardPostsEntity entity = boardPostsRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        
        // 작성자 확인 또는 권한 확인 로직 추가
        EmployeeEntity employee = employeeRepository.findByLoginId(loginId)
            .orElseThrow(() -> new RuntimeException("사원 정보를 찾을 수 없습니다."));
            
        if (entity.getEmployee().getEmpId() != employee.getEmpId() &&
            !isManagerRole(employee)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }
        
        entity.setBoardTitle(dto.getBoardTitle());
        entity.setBoardContent(dto.getBoardContent());
        
        TblBoardPostsEntity updatedEntity = boardPostsRepository.save(entity);
        return new BoardPostResponseDTO(updatedEntity);
    }
    
    // 게시글 삭제
    @Transactional
    public void deleteBoardPost(int postId, String loginId) {
        TblBoardPostsEntity entity = boardPostsRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        
        // 작성자 확인 또는 권한 확인 로직 추가
        EmployeeEntity employee = employeeRepository.findByLoginId(loginId)
            .orElseThrow(() -> new RuntimeException("사원 정보를 찾을 수 없습니다."));
            
        if (entity.getEmployee().getEmpId() != employee.getEmpId() &&
            !isManagerRole(employee)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }
        
        boardPostsRepository.delete(entity);
    }
    
    // 답변 등록
    @Transactional
    public BoardCommentResponseDTO createBoardComment(TblBoardCommentsDTO dto, String loginId) {
        TblBoardPostsEntity post = boardPostsRepository.findById(dto.getPostId())
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        
        // 권한 확인 로직 추가
        EmployeeEntity employee = employeeRepository.findByLoginId(loginId)
            .orElseThrow(() -> new RuntimeException("사원 정보를 찾을 수 없습니다."));
            
        if (!isManagerRole(employee)) {
            throw new RuntimeException("답변 등록 권한이 없습니다.");
        }
        
        TblBoardCommentsEntity entity = new TblBoardCommentsEntity(dto);
        entity.setPost(post);
        entity.setComCreatedAt(LocalDateTime.now());
        
        TblBoardCommentsEntity savedEntity = boardCommentsRepository.save(entity);

        if ((post.getBoardType() == 2 || post.getBoardType() == 3) &&
                post.getEmployee() != null &&
                post.getEmployee().getEmpId() != employee.getEmpId()) {

            try {
                String content = "[게시판] 작성하신 글에 답변이 등록되었습니다.";
                String link = switch (post.getBoardType()) {
                    case 2 -> "/headquarters/board/suggestions";
                    case 3 -> "/headquarters/board/store-inquiries";
                    default -> "/";
                };

                notificationService.createNotification(
                        post.getEmployee().getEmpId(),
                        3,
                        "BOARD_REPLY",
                        "INFO",
                        content,
                        link
                );
            } catch (Exception e) {
                log.error("[알림] 댓글 알림 생성 실패: {}", e.getMessage(), e);
            }
        }

        BoardCommentResponseDTO responseDTO = new BoardCommentResponseDTO(savedEntity);
        responseDTO.setEmpName(employee.getEmpName());

        return responseDTO;
    }

    // 관리자 권한 확인 (HQ_BR, HQ_BR_M, MASTER)
    private boolean isManagerRole(EmployeeEntity employee) {
        if (employee.getDepartment() == null) return false;
        
        String deptName = employee.getDepartment().getDeptName();
        return deptName.equals("HQ_BR") || 
               deptName.equals("HQ_BR_M") || 
               deptName.equals("MASTER");
    }
}