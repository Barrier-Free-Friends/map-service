package org.bf.mapservice.mapservice.global.security;

import lombok.RequiredArgsConstructor;
import org.bf.global.security.LoginFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class MapServiceSecurityConfig {

    private final LoginFilter loginFilter; // 서브모듈에서 온 필터 그대로 재사용

    /**
     * /routes/** 전용 SecurityFilterChain
     * 서브모듈 filterChain 보다 먼저 적용되도록 @Order(0)
     */
    @Bean
    @Order(0)
    public SecurityFilterChain routesFilterChain(HttpSecurity http) throws Exception {
        http
                // 이 체인은 /routes/** 에만 매칭됨
                .securityMatcher("/routes/**")
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * 이 서비스에서만 사용할 CORS 설정
     * /routes/** 에만 적용되도록 매핑
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 프론트 도메인 허용
        config.setAllowedOrigins(List.of(
                "http://localhost:5173"   // Vite dev
                // 나중에 배포 도메인도 추가
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // /routes/** 에만 CORS 설정 적용
        source.registerCorsConfiguration("/routes/**", config);
        return source;
    }
}
