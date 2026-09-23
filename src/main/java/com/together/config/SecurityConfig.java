package com.together.config;

import com.together.common.security.JwtAuthenticationFilter;
import com.together.common.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 인증은 카카오 소셜 로그인으로 발급한 자체 JWT만 사용한다(stateless).
 *
 * <p>인증 없이 열어두는 경로는 Notion API 명세서의 {@code Auth Required = N} 항목과 문서/헬스체크뿐이다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 인증 없이 여는 업무 엔드포인트. 셋 다 "아직/이미 우리 JWT 가 없는 상태"에서 불린다. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/kakao",
            // FR-035 액세스 토큰이 이미 만료된 상태에서 부르는 API 라 인증을 요구할 수 없다.
            "/auth/refresh",
            // 카카오 서버가 부른다. 우리 JWT 가 아니라 Admin 키로 검증한다(AuthService).
            "/auth/kakao/unlink-callback"
    };

    private static final String[] DOC_ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/health/**"
    };

    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(DOC_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                // FR-035 만료/무효 토큰은 401 → 프론트 인터셉터가 로그인 화면으로 이동시킨다
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins.isEmpty()) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
