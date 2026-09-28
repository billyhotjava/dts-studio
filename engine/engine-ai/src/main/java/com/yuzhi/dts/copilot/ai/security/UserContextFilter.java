package com.yuzhi.dts.copilot.ai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter that extracts user identity headers and populates the
 * {@link CopilotUserContextHolder} ThreadLocal for downstream use.
 * Runs after {@link ApiKeyAuthFilter}.
 */
@Component
public class UserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-DTS-User-Id";
    private static final String HEADER_USER_NAME = "X-DTS-User-Name";
    private static final String HEADER_DISPLAY_NAME = "X-DTS-Display-Name";
    private static final String HEADER_ROLES = "X-DTS-Roles";
    private static final String HEADER_DEPT = "X-DTS-Dept";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof ApiKeyAuthentication apiKeyAuth) {
                String userId = request.getHeader(HEADER_USER_ID);
                String userName = request.getHeader(HEADER_USER_NAME);
                String displayName = request.getHeader(HEADER_DISPLAY_NAME);
                String rolesHeader = request.getHeader(HEADER_ROLES);
                String dept = request.getHeader(HEADER_DEPT);

                // If no X-DTS-User-Id header, default to the API key name
                if (userId == null || userId.isBlank()) {
                    userId = apiKeyAuth.getApiKey().getName();
                }
                if (userName == null || userName.isBlank()) {
                    userName = userId;
                }
                if (displayName == null || displayName.isBlank()) {
                    displayName = userName;
                }

                List<String> roles = (rolesHeader != null && !rolesHeader.isBlank())
                    ? Arrays.asList(rolesHeader.split(","))
                    : List.of();

                CopilotUserContext userContext = new CopilotUserContext(
                    userId, userName, displayName, roles, dept,
                    String.valueOf(apiKeyAuth.getApiKey().getId())
                );

                CopilotUserContextHolder.set(userContext);

                // Update the authentication with the enriched user context
                ApiKeyAuthentication enrichedAuth = new ApiKeyAuthentication(
                    apiKeyAuth.getApiKey(), userContext
                );
                SecurityContextHolder.getContext().setAuthentication(enrichedAuth);
            }

            filterChain.doFilter(request, response);
        } finally {
            CopilotUserContextHolder.clear();
        }
    }
}
