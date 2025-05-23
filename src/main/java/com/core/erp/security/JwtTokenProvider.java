package com.core.erp.security;

import com.core.erp.domain.EmployeeEntity;
import com.core.erp.dto.CustomPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtTokenProvider {

    private final String secretKey = "your-very-secret-key-should-be-long-enough-1234567890";
    private final long validityInMilliseconds = 24 * 60 * 60 * 1000; // 1시간

    public String createToken(EmployeeEntity employee) {
        Claims claims = Jwts.claims().setSubject(employee.getLoginId());
        claims.put("empId", employee.getEmpId());
        claims.put("deptId", employee.getDepartment().getDeptId());
        claims.put("empName", employee.getEmpName());
        claims.put("deptName", employee.getDepartment().getDeptName());
    
        // role 클레임 추가
        claims.put("role", "ROLE_" + mapDeptIdToRole(employee.getDepartment().getDeptId()));
    
        // store 정보 추가 (null이 아닌 경우에만)
        if (employee.getStore() != null) {
            claims.put("storeId", employee.getStore().getStoreId());
            claims.put("storeName", employee.getStore().getStoreName());
        } else {
            claims.put("storeId", null);
            claims.put("storeName", null);
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }


    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey.getBytes()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String loginId = claims.getSubject();
        Integer empId = claims.get("empId", Integer.class);
        Integer deptId = claims.get("deptId", Integer.class);
        Integer storeId = claims.get("storeId", Integer.class);
        String mappedRole = mapDeptIdToRole(deptId);
        String authorityRole = "ROLE_" + mappedRole;

        CustomPrincipal principal = new CustomPrincipal(loginId, empId, deptId, storeId, mappedRole);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authorityRole));

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }


    public String mapDeptIdToRole(Integer deptId) {
        // 부서 ID → 권한명 매핑 (예시)
        return switch (deptId) {
            case 1 -> "NON_STORE";
            case 2 -> "NON_HQ";
            case 3 -> "STORE";
            case 4, 5 -> "HQ_HRM";
            case 6, 7 -> "HQ_PRO";
            case 8, 9 -> "HQ_BR";
            case 10 -> "MASTER";
            default -> "USER";
        };
    }
}