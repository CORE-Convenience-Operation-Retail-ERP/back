package com.core.erp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    
    // 인증이 필요하지 않은 경로 목록
    private final List<String> excludedPaths = Arrays.asList(
            "/api/auth/login", 
            "/api/auth/register",
            "/api/auth/check-email",
            "/api/auth/send-verification-email",  // 이메일 인증 코드 발송
            "/api/auth/verify-email",             // 이메일 인증 코드 확인
            "/api/public/**",
            "/ws",                                // WebSocket 엔드포인트
            "/ws/**"                              // WebSocket 하위 경로
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 요청 정보 로깅
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("===== JwtAuthenticationFilter 실행 =====");
        System.out.println("요청 URI: " + uri);
        System.out.println("요청 메서드: " + method);
        
        // 로그인 요청 특별 처리
        if (uri.equals("/api/auth/login")) {
            System.out.println("===== 로그인 요청 감지 =====");
            
            // 요청 본문 내용 로깅 (디버깅용)
            try {
                String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                System.out.println("로그인 요청 본문: " + requestBody);
                
                // 원래의 요청 본문을 다시 읽을 수 있도록 래핑
                request = new CustomHttpServletRequestWrapper(request, requestBody);
            } catch (Exception e) {
                System.err.println("로그인 요청 본문 읽기 실패: " + e.getMessage());
            }
        }
        
        // 이메일 인증 API 요청 로깅
        boolean isEmailVerificationRequest = uri.equals("/api/auth/send-verification-email") || 
                                           uri.equals("/api/auth/verify-email");
        if (isEmailVerificationRequest) {
            System.out.println("===== 이메일 인증 API 요청 감지 =====");
        }
        
        // 근태 관리 API 추가 로깅
        boolean isHrApiRequest = uri.startsWith("/api/hr/");
        if (isHrApiRequest) {
            System.out.println("===== 근태 관리 API 요청 감지 =====");
        }
        
        // 인증이 필요하지 않은 경로인지 확인
        boolean isExcludedPath = excludedPaths.stream().anyMatch(path -> {
            if (path.endsWith("/**")) {
                String basePath = path.substring(0, path.length() - 3);
                return uri.startsWith(basePath);
            }
            return uri.equals(path);
        });
        
        if (isExcludedPath) {
            System.out.println("인증이 필요 없는 경로: " + uri + " - JWT 필터 건너뜀");
            if (isEmailVerificationRequest) {
                System.out.println("이메일 인증 API 요청 처리 중 - 인증 없이 진행");
            }
            filterChain.doFilter(request, response);
            return;
        }
        
        // JWT 토큰 확인
        String token = jwtTokenProvider.resolveToken(request);
        System.out.println("추출된 토큰: " + (token != null ? "존재함" : "없음"));
        
        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                System.out.println("토큰 유효성 검증 성공");
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                
                // 인증 객체 로깅
                if (auth != null) {
                    System.out.println("인증된 사용자: " + auth.getName());
                    System.out.println("사용자 권한: " + auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(", ")));
                    System.out.println("Principal 타입: " + auth.getPrincipal().getClass().getName());
                    
                    // 근태 관리 API 접근 시 권한 확인
                    if (isHrApiRequest) {
                        boolean hasHqRole = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().startsWith("ROLE_HQ"));
                        boolean hasMasterRole = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_MASTER"));
                        boolean hasStoreRole = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_STORE"));
                        
                        System.out.println("근태 관리 API 접근: 본사 권한 보유 = " + hasHqRole);
                        System.out.println("근태 관리 API 접근: 마스터 권한 보유 = " + hasMasterRole);
                        System.out.println("근태 관리 API 접근: 점주 권한 보유 = " + hasStoreRole);
                        
                        if (!hasHqRole && !hasMasterRole && !hasStoreRole) {
                            System.out.println("경고: 근태 관리 API 접근 권한 없음! 403 오류 예상됨");
                        }
                    }
                    
                    // 게시판 관련 API 호출 시 추가 로깅
                    if (uri.contains("/api/headquarters/board/")) {
                        System.out.println("게시판 API 접근 감지 - 상세 권한 확인");
                        System.out.println("ROLE_MASTER 권한 보유: " + 
                            auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_MASTER")));
                    }
                } else {
                    System.out.println("인증 객체 생성 실패 (auth가 null)");
                }
                
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else if (token != null) {
                System.out.println("토큰 유효성 검증 실패");
            }
        } catch (Exception e) {
            System.err.println("토큰 처리 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
        
        // 요청 처리 후 결과 로깅
        if (isHrApiRequest) {
            System.out.println("===== 근태 관리 API 응답 코드: " + response.getStatus() + " =====");
        }
        if (isEmailVerificationRequest) {
            System.out.println("===== 이메일 인증 API 응답 코드: " + response.getStatus() + " =====");
        }
    }
}