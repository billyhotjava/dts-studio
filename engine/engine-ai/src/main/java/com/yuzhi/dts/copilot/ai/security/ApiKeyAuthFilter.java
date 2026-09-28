package com.yuzhi.dts.copilot.ai.security;

import com.yuzhi.dts.copilot.ai.domain.ApiKey;
import com.yuzhi.dts.copilot.ai.service.auth.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filter that extracts and validates API keys from the Authorization header.
 * Sets the SecurityContext with an {@link ApiKeyAuthentication} on success.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> WHITELISTED_PATHS = List.of(
        "/actuator/**",
        "/api/health",
        "/api/info",
        "/api/ai/config/**",
        "/api/ai/nl2sql/**",
        "/api/ai/copilot/datasources/**",
        "/api/ai/copilot/screen/**",
        "/api/auth/keys/**",
        "/internal/**"
    );

    private final ApiKeyService apiKeyService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiKeyAuthFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    private boolean isWhitelisted(String path) {
        return WHITELISTED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Whitelisted paths: skip auth and continue filter chain
        if (isWhitelisted(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String rawKey = authHeader.substring(BEARER_PREFIX.length()).trim();
        Optional<ApiKey> optKey = apiKeyService.validateKey(rawKey);

        if (optKey.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid, expired, or revoked API key\"}");
            return;
        }

        ApiKey apiKey = optKey.get();

        // Increment usage asynchronously-safe (within same transaction boundary is fine here)
        apiKeyService.incrementUsage(apiKey.getId());

        // Build a minimal user context from the API key itself; UserContextFilter will enrich it
        CopilotUserContext userContext = new CopilotUserContext(
            apiKey.getName(), apiKey.getName(), apiKey.getName(),
            List.of(), null, String.valueOf(apiKey.getId())
        );

        ApiKeyAuthentication authentication = new ApiKeyAuthentication(apiKey, userContext);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authenticated request with API key id={} prefix={}", apiKey.getId(), apiKey.getKeyPrefix());

        filterChain.doFilter(request, response);
    }
}
