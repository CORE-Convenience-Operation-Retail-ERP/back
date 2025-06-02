package com.core.erp.security;

import com.core.erp.dto.CustomPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = getTokenFromHeader(accessor);
            
            // 토큰이 없는 경우 연결 거부
            if (token == null) {
                System.out.println("웹소켓 연결 거부: 토큰 없음");
                return null; // 토큰 없이는 웹소켓 연결 불허
            }
            
            if (jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                
                // 본사 직원이 아닌 경우 접근 거부 (depart_id 4~10만 허용)
                if (isHeadquartersEmployee(authentication)) {
                    accessor.setUser(authentication);
                    System.out.println("웹소켓 연결 인증 성공: " + authentication.getName());
                } else {
                    System.out.println("웹소켓 연결 거부: 본사 직원이 아님");
                    return null; // 본사 직원이 아닌 경우 연결 거부
                }
            } else {
                System.out.println("웹소켓 연결 거부: 인증 토큰 유효하지 않음");
                return null; // 인증 토큰이 유효하지 않으면 연결 거부
            }
        }
        
        return message;
    }
    
    private String getTokenFromHeader(StompHeaderAccessor accessor) {
        // Authorization 헤더에서 토큰을 추출
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String bearerToken = authorization.get(0);
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        return null;
    }
    
    private boolean isHeadquartersEmployee(Authentication authentication) {
        if (authentication == null) return false;
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomPrincipal) {
            Integer deptId = ((CustomPrincipal) principal).getDeptId();
            return deptId != null && deptId >= 3 && deptId <= 10;
        }
        
        // 권한 기반 확인 (대체 방법)
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> 
                    auth.equals("ROLE_HQ_HRM") || 
                    auth.equals("ROLE_HQ_PRO") || 
                    auth.equals("ROLE_HQ_BR") || 
                    auth.equals("ROLE_MASTER"));
    }
} 