package com.yuzhi.dts.copilot.ai.security;

import com.yuzhi.dts.copilot.ai.domain.ApiKey;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

/**
 * Authentication token representing a validated API key.
 */
public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final ApiKey apiKey;
    private final CopilotUserContext userContext;

    public ApiKeyAuthentication(ApiKey apiKey, CopilotUserContext userContext) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
        this.userContext = userContext;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey.getKeyPrefix();
    }

    @Override
    public Object getPrincipal() {
        return apiKey.getName();
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public CopilotUserContext getUserContext() {
        return userContext;
    }
}
