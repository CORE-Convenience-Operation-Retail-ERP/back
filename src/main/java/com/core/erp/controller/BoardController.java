// CORE-ERP-POS-Backend/src/main/java/com/core/erp/controller/BoardController.java
package com.core.erp.controller;

import com.core.erp.dto.*;
import com.core.erp.dto.BoardCommentResponseDTO;
import com.core.erp.dto.BoardPostResponseDTO;
import com.core.erp.dto.TblBoardCommentsDTO;
import com.core.erp.dto.TblBoardPostsDTO;
import com.core.erp.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/headquarters/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 게시판 타입별 게시글 목록 조회
    @GetMapping("/{boardType}")
    public ResponseEntity<List<BoardPostResponseDTO>> getBoardPosts(@PathVariable int boardType) {
        return ResponseEntity.ok(boardService.getBoardPostsByType(boardType));
    }

    // 게시글 단일 조회
    @GetMapping("/post/{postId}")
    public ResponseEntity<BoardPostResponseDTO> getBoardPost(@PathVariable int postId) {
        return ResponseEntity.ok(boardService.getBoardPost(postId));
    }

    // 게시판 최근 게시글 조회 (위젯용)
    @GetMapping("/recent")
    public ResponseEntity<List<BoardPostResponseDTO>> getRecentPosts() {
        return ResponseEntity.ok(boardService.getRecentPosts(4)); // 최근 4개 게시글
    }

    // 게시글 등록
    @PostMapping("/write")
    public ResponseEntity<BoardPostResponseDTO> createBoardPost(
            @RequestBody TblBoardPostsDTO dto,
            Authentication authentication) {

        String loginId;
        if (authentication.getPrincipal() instanceof CustomPrincipal) {
            CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
            loginId = principal.getLoginId();
        } else {
            loginId = authentication.getName();
        }

        // 권한 체크
        boolean isStore = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STORE"));
        boolean isHqBr = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HQ_BR"));
        boolean isMaster = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MASTER"));

        int boardType = dto.getBoardType(); // DTO에 boardType 필드가 있다고 가정

        // STORE 권한은 건의사항(2), 지점문의(3)만 작성 가능
        if (isStore && !(boardType == 2 || boardType == 3)) {
            throw new AccessDeniedException("점주는 건의사항/지점문의만 작성할 수 있습니다.");
        }
        // HQ_BR, MASTER는 모든 게시판 작성 가능 (추가 체크 필요 없음)
        // 그 외 권한은 작성 불가
        if (!isStore && !isHqBr && !isMaster) {
            throw new AccessDeniedException("작성 권한이 없습니다.");
        }

        return ResponseEntity.ok(boardService.createBoardPost(dto, loginId));
    }

    // 게시글 수정
    @PutMapping("/write/{postId}")
    public ResponseEntity<BoardPostResponseDTO> updateBoardPost(
            @PathVariable int postId,
            @RequestBody TblBoardPostsDTO dto,
            Authentication authentication) {

        // CustomPrincipal 객체에서 loginId 추출
        String loginId;
        if (authentication.getPrincipal() instanceof CustomPrincipal) {
            CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
            loginId = principal.getLoginId();
        } else {
            loginId = authentication.getName();
        }

        return ResponseEntity.ok(boardService.updateBoardPost(postId, dto, loginId));
    }

    // 게시글 삭제
    @DeleteMapping("/write/{postId}")
    public ResponseEntity<Void> deleteBoardPost(
            @PathVariable int postId,
            Authentication authentication) {

        // CustomPrincipal 객체에서 loginId 추출
        String loginId;
        if (authentication.getPrincipal() instanceof CustomPrincipal) {
            CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
            loginId = principal.getLoginId();
        } else {
            loginId = authentication.getName();
        }

        boardService.deleteBoardPost(postId, loginId);
        return ResponseEntity.noContent().build();
    }

    // 게시글 답변 등록
    @PostMapping("/comment")
    public ResponseEntity<BoardCommentResponseDTO> createBoardComment(
            @RequestBody TblBoardCommentsDTO dto,
            Authentication authentication) {

        // CustomPrincipal 객체에서 loginId 추출
        String loginId;
        if (authentication.getPrincipal() instanceof CustomPrincipal) {
            CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
            loginId = principal.getLoginId();
        } else {
            loginId = authentication.getName();
        }

        return ResponseEntity.ok(boardService.createBoardComment(dto, loginId));
    }
}