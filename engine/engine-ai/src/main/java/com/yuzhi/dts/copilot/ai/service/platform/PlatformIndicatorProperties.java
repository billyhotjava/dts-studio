package com.yuzhi.dts.copilot.ai.service.platform;

public record PlatformIndicatorProperties(
        String baseUrl,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String authToken,
        String serviceName,
        String serviceToken,
        int timeoutSeconds
) {
    public PlatformIndicatorProperties {
        timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
    }
}
