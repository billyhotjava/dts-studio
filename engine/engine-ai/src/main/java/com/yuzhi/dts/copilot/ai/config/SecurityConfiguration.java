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
 * <p>
 * Spring Security handles CSRF disable + stateless sessions.
 * Actual auth is enforced at application layer:
 * - ApiKeyAuthFilter: validates Bearer API key for regular endpoints
 * - checkAdminSecret: validates X-Admin-Secret for admin endpoints
 * - Whitelisted paths (actuator, health, config, auth/keys) skip API key check
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
                .anyRequest().permitAll()
            )
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(userContextFilter, ApiKeyAuthFilter.class);

        return http.build();
    }
}
