package com.rag.project.api;

import com.rag.project.api.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (JWT는 세션을 사용하지 않으므로 CSRF 공격에 상대적으로 안전)
                .csrf(csrf -> csrf.disable())

                // 세션 관리 정책 설정 -> "STATELESS" (상태 없음)
                // 이것이 Spring Security가 세션을 생성하지 않고, 오직 JWT 토큰으로만 인증하게 만드는 핵심 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // --- [ 인증 없이 접근 허용 ] ---
                        .requestMatchers(
                                "/api/members/signup", // 회원가입 API
                                "/api/members/login",  // 로그인 API
                               // "/hello",              // 테스트용 API
                                "/swagger-ui.html",    // Swagger UI
                                "/swagger-ui/**",      // (Swagger UI 리소스)
                                "/v3/api-docs/**",     // (Swagger API 문서)
                                "/h2-console/**"       // H2 콘솔
                        ).permitAll() // -> 이 경로들은 '누구나' 접근 가능

                        // --- [ 인증이 필요한 접근 ] ---
                        .anyRequest() // -> 위에서 허용한 경로 외 '모든' 요청은
                        .authenticated() // -> '반드시 인증(로그인)'되어야 함
                )

                // H2 콘솔 frame 허용
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )

                // 만든 jwtAuthFilter를
                // Spring Security의 기본 인증 필터(UsernamePasswordAuthenticationFilter) '앞에' 배치
                // -> "기본 로그인 쓰지 말고, 우리가 만든 JWT 필터 먼저 씀"
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
