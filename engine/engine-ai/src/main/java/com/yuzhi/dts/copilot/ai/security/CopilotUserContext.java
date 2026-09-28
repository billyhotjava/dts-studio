package com.yuzhi.dts.copilot.ai.security;

import java.util.List;

/**
 * Immutable record holding the identity and context of the current user.
 */
public record CopilotUserContext(
    String userId,
    String userName,
    String displayName,
    List<String> roles,
    String dept,
    String apiKeyId
) {}
