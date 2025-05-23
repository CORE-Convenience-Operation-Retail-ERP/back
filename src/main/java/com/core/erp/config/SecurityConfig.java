package com.core.erp.config;

import com.core.erp.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Spring Security 설정 클래스
 * 이 클래스는 애플리케이션의 보안 설정을 담당합니다.
 * JWT 기반 인증 및 경로별 권한 설정을 정의합니다.
 */
@Configuration // 스프링 설정 클래스임을 나타냄
@EnableWebSecurity // Spring Security 활성화
@RequiredArgsConstructor // 생성자 주입을 위한 lombok 어노테이션
public class SecurityConfig {

    // JWT 인증 필터 주입
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 권한별 경로 리스트
    public static final String[] HQ_HRM_PATHS = {
            "/api/headquarters/hr/**",           // 본사 인사팀 전용 API 전체
            "/api/hr/annual-leave/**",           // 연차 관리(신청, 승인 등)
            "/api/employees/**",                 // 사원 정보 조회
            "/api/employee-management/**",       // 사원 관리(등록, 수정 등)
            "/api/departments",                  // 부서 정보 조회
            "/api/stores/owners",                // 점주 목록 조회
            "/api/hr/attendance/**",             // 근태 관리
            "/api/hr/my-salary",                 // 본인 급여 내역 조회
            // 필요시 추가
    };

    public static final String[] HQ_PRO_PATHS = {
            // HQ_PRO 권한이 접근 가능한 경로
            "/api/products/all",
            "/api/products/paged/**",
            "/api/products/detail/**",
            "/api/categories/tree",
            "/api/stock/summary",
            "/api/stock/detail/**",
            "/api/products/register",
            "/api/products/upload-image",
            "/api/products/edit/**",
            "/api/products/delete/**",
            "/api/products/search/**",
            "/api/products/filter/**",
            "/api/headquarters/products/**",
            "/api/hq-stock/**",
    };

    public static final String[] HQ_BR_PATHS = {
            "/api/headquarters/branches/**",     // 지점 관리(조회, 등록, 수정, 삭제)
            "/api/store-inquiries/**",           // 점포 문의글 관리
            "/api/headquarters/board/comment/**",// 게시판 답변 등록
            "/api/headquarters/board/write/**", // 게시판 글 작성


            // 필요시 추가
    };

    public static final String[] STORE_PATHS = {
            "/api/store/**",
            "/api/parttimer-schedule/**",
            "/api/display-location/**",
            "/api/erp/disposal/**",
            "/api/order/**",
            "/api/attendance/part-timer/**",
            "/api/display-mapping/**",
            "/api/pos/**",
            "/api/parttimer-schedule/**",
            "/api/salary/**",
            "/api/transactions/**",
            "/api/erp/settlement/**",
            "/api/erp/statistics/**",
            "/api/attendance/part-timer/**",
            "/api/products/**",
    };

    public static final String[] HQ_COMMON_READ_PATHS = {
            "/api/products/all",
            "/api/products/paged/**",
            // "/api/products/detail/**",
            "/api/hq-stock",
            "/api/hq-stock/**",
            "/api/headquarters/branches",
            "/api/headquarters/branches/**",
            "/api/headquarters/notice/**",
            "/api/headquarters/statistics/**",
            "/api/sales/analysis/**",
            "/api/integrated-stock/**",
            "/api/branches/stock/**",
            "/api/customer/**",
            "/api/departments",
            "/api/employees",
            "/api/employees/{empId}",
            "/api/stores",
            "/api/stores/owners",
            "/api/store-inquiries/**",
            "/api/dashboard/**",
            "/api/employee-management/{empId}",
            "/api/store-owners/{empId}",
            "/api/stores/list",
            "/api/category/**",
            "/api/stock-adjust/**",
            "/api/annual-leave/**",
            "/api/transaction/**",
            "/api/chat/**",
            "/api/hr/**",
            "/api/integrated-stock/**",
            "/api/store-inquiries/**",

    };

    public static final String[] COMMON_READ_PATHS = {
            "/api/headquarters/board/**",
            "/api/headquarters/board/comment/**",
            "/api/headquarters/board/post/**",
            "/api/categories/**",
            "/api/stock/**",
            "/api/stock-flow/**",
            "/api/products/**",
    };




    /**
     * 보안 필터 체인 설정
     * 모든 HTTP 요청에 대한 보안 규칙을 정의합니다.email_token
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("==== SecurityConfig 설정 로드 중 ====");

        http
                // CSRF 보호 기능 비활성화 (REST API에서는, JWT 같은 토큰 기반 인증을 사용할 때 일반적으로 비활성화함)
                .csrf(csrf -> csrf.disable())

                // CORS 설정 (Cross-Origin Resource Sharing)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    // 명시적 도메인 목록 설정
                    config.setAllowedOrigins(List.of(
                            "http://localhost:3000",
                            "http://localhost:3001",
                            "http://localhost:8080",
                            "http://127.0.0.1:3000",
                            "http://127.0.0.1:3001",
                            "http://127.0.0.1:8080"
                    )); // 특정 출처 허용
                    config.setAllowedMethods(List.of("GET","PATCH" ,"POST", "PUT", "DELETE", "OPTIONS")); // 허용할 HTTP 메서드
                    config.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
                    config.setExposedHeaders(List.of("Authorization")); // 클라이언트에 노출할 헤더
                    config.setAllowCredentials(true); // 인증 정보 포함 허용
                    config.setMaxAge(3600L); // preflight 캐시 시간 (초)
                    return config;
                }))

                // 세션 관리 설정 - JWT 사용하므로 세션은 STATELESS로 설정
                // STATELESS: 서버가 세션을 생성하지 않고 각 요청을 독립적으로 처리함
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL 패턴별 접근 권한 설정
                // ⚠️ 주의: 설정 순서가 중요합니다. 더 구체적인 경로를 먼저 정의하고, 일반적인 경로를 나중에 정의해야 합니다.
                .authorizeHttpRequests(auth -> auth
                        // 1. 인증 없이 접근 가능한 경로 설정 (로그인, 등록, 정적 리소스 등)
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/check-email",
                                "/api/auth/send-verification-email",  // 이메일 인증 코드 발송
                                "/api/auth/verify-email",            // 이메일 인증 코드 확인
                                "/css/**",
                                "/js/**",
                                "/images/**",        // 정적 리소스
                                "/api/products/all",
                                "/api/categories/tree", // 상품 정보 조회 API
                                "/api/barcode",
                                "/api/customer/**",  // 모든 고객 관련 API 허용
                                "/ws/**"            // WebSocket 엔드포인트 허용
                        ).permitAll() // 모든 사용자 접근 허용


                        .requestMatchers(HttpMethod.GET, HQ_COMMON_READ_PATHS).hasAnyRole("HQ", "HQ_HRM", "HQ_HRM_M", "HQ_PRO", "HQ_PRO_M", "HQ_BR", "HQ_BR_M","MASTER")
                        .requestMatchers(HttpMethod.GET, COMMON_READ_PATHS).hasAnyRole("HQ", "HQ_HRM", "HQ_HRM_M", "HQ_PRO", "HQ_PRO_M", "HQ_BR", "HQ_BR_M","STORE","MASTER")
                        .requestMatchers(HttpMethod.POST, "/api/headquarters/board/write").hasAnyRole("STORE","HQ_BR","HQ_BR_M","MASTER")
                        .requestMatchers(HQ_PRO_PATHS).hasAnyRole("HQ_PRO", "HQ_PRO_M", "MASTER")
                        .requestMatchers(HQ_HRM_PATHS).hasAnyRole("HQ_HRM", "HQ_HRM_M", "MASTER")
                        .requestMatchers(HQ_BR_PATHS).hasAnyRole("HQ_BR", "HQ_BR_M", "MASTER")
                        .requestMatchers(STORE_PATHS).hasAnyRole("STORE", "MASTER")
                        .requestMatchers("/api/hr/annual-leave/request").hasAnyRole("HQ", "HQ_HRM", "HQ_HRM_M", "HQ_PRO", "HQ_PRO_M", "HQ_BR", "HQ_BR_M","MASTER")
                        // 그 외 모든 요청은 인증 필요 (기본 설정)
                        .anyRequest().authenticated() // 명시되지 않은 모든 URL은 인증된 사용자만 접근 가능
                );


        // JWT 인증 필터 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("==== SecurityConfig 설정 완료 ====");

        return http.build();
    }

    /**
     * 인증 관리자 빈 설정
     * JWT 토큰 기반 인증에 사용됩니다.
     *
     * @param configuration 인증 설정
     * @return AuthenticationManager 객체
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}