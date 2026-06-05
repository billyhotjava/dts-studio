package com.yuzhi.dts.copilot.ai.config;

import com.yuzhi.dts.copilot.ai.service.platform.PlatformSemanticDraftProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlatformSemanticDraftConfiguration {

    @Bean
    PlatformSemanticDraftProperties platformSemanticDraftProperties(
            @Value("${copilot.platform.semantic-draft.base-url:${copilot.platform.indicator.base-url:}}") String baseUrl,
            @Value("${copilot.platform.semantic-draft.auth-token:${copilot.platform.indicator.auth-token:}}") String authToken,
            @Value("${copilot.platform.semantic-draft.service-name:${copilot.platform.indicator.service-name:dts-copilot}}") String serviceName,
            @Value("${copilot.platform.semantic-draft.service-token:${copilot.platform.indicator.service-token:${DTS_PLATFORM_SERVICE_TOKEN:${DTS_INBOUND_FROM_COPILOT:${DTS_INBOUND_FROM_ANALYTICS:${DTS_ADMIN_SERVICE_TOKEN:}}}}}}") String serviceToken,
            @Value("${copilot.platform.semantic-draft.timeout-seconds:${copilot.platform.indicator.timeout-seconds:10}}") int timeoutSeconds) {
        return new PlatformSemanticDraftProperties(baseUrl, authToken, serviceName, serviceToken, timeoutSeconds);
    }
}
