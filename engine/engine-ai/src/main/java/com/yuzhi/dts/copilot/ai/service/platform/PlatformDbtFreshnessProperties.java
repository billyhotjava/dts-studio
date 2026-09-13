package com.yuzhi.dts.copilot.ai.service.platform;

public record PlatformDbtFreshnessProperties(
        boolean enabled,
        String baseUrl,
        String authToken,
        String serviceName,
        String serviceToken,
        String activeDept,
        int timeoutSeconds,
        int staleAfterHours
) {
    public PlatformDbtFreshnessProperties {
        timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
        staleAfterHours = staleAfterHours > 0 ? staleAfterHours : 24;
    }
}
