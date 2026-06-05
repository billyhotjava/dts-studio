package com.yuzhi.dts.copilot.ai.service.platform;

public record PlatformSemanticDraftProperties(
        String baseUrl,
        String authToken,
        String serviceName,
        String serviceToken,
        int timeoutSeconds) {

    public PlatformSemanticDraftProperties {
        baseUrl = normalize(baseUrl);
        authToken = normalize(authToken);
        serviceName = normalize(serviceName);
        serviceToken = normalize(serviceToken);
        timeoutSeconds = timeoutSeconds <= 0 ? 10 : timeoutSeconds;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
