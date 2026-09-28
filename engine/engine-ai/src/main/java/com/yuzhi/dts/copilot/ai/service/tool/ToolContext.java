package com.yuzhi.dts.copilot.ai.service.tool;

/**
 * Contextual information passed to tools during execution.
 *
 * @param userId       the authenticated user identifier
 * @param sessionId    the current chat session identifier
 * @param dataSourceId the active data source identifier (may be {@code null})
 */
public record ToolContext(String userId, String sessionId, Long dataSourceId) {}
