package com.yuzhi.dts.copilot.ai.config;

import com.yuzhi.dts.copilot.ai.security.ApiKeyAuthFilter;
import com.yuzhi.dts.copilot.ai.security.UserContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Copilot AI service.
 * Enforces stateless API-key-based authentication on all endpoints
 * except health/actuator endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final UserContextFilter userContextFilter;

    public SecurityConfiguration(ApiKeyAuthFilter apiKeyAuthFilter, UserContextFilter userContextFilter) {
        this.apiKeyAuthFilter = apiKeyAuthFilter;
        this.userContextFilter = userContextFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/health", "/api/info").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(userContextFilter, ApiKeyAuthFilter.class);

        return http.build();
    }
}
