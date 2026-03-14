package com.yuzhi.dts.copilot.analytics.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Filter that extracts and validates API keys via copilot-ai.
 * Sets the SecurityContext with an authentication token on success.
 */
@Component
public class AnalyticsApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsApiKeyAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> WHITELISTED_PATHS = List.of(
            "/actuator/**",
            "/api/health",
            "/api/info",
            "/api/public/**"
    );

    private final ApiKeyAuthService apiKeyAuthService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AnalyticsApiKeyAuthFilter(ApiKeyAuthService apiKeyAuthService) {
        this.apiKeyAuthService = apiKeyAuthService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return WHITELISTED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String rawKey = authHeader.substring(BEARER_PREFIX.length()).trim();
        Optional<ApiKeyAuthService.AuthenticatedUser> authResult = apiKeyAuthService.authenticate(rawKey);

        if (authResult.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid, expired, or revoked API key\"}");
            return;
        }

        ApiKeyAuthService.AuthenticatedUser authenticatedUser = authResult.get();
        AnalyticsAuthentication authentication = new AnalyticsAuthentication(authenticatedUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authenticated analytics request for user id={}", authenticatedUser.user().getId());

        filterChain.doFilter(request, response);
    }

    /**
     * Authentication token for analytics requests.
     */
    public static class AnalyticsAuthentication extends AbstractAuthenticationToken {

        private final ApiKeyAuthService.AuthenticatedUser authenticatedUser;

        public AnalyticsAuthentication(ApiKeyAuthService.AuthenticatedUser authenticatedUser) {
            super(Collections.emptyList());
            this.authenticatedUser = authenticatedUser;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return authenticatedUser.apiKey();
        }

        @Override
        public Object getPrincipal() {
            return authenticatedUser.user();
        }

        public ApiKeyAuthService.AuthenticatedUser getAuthenticatedUser() {
            return authenticatedUser;
        }
    }
}
