package com.yuzhi.dts.copilot.ai.config;

import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogSyncProperties;
import com.yuzhi.dts.copilot.ai.service.platform.PlatformIndicatorProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class PlatformIndicatorConfiguration {

    @Bean
    IndicatorCatalogSyncProperties indicatorCatalogSyncProperties(
            @Value("${copilot.platform.indicator.sync.enabled:true}") boolean enabled,
            @Value("${copilot.platform.indicator.sync.page-size:100}") int pageSize,
            @Value("${copilot.platform.indicator.sync.max-pages:200}") int maxPages) {
        return new IndicatorCatalogSyncProperties(enabled, pageSize, maxPages);
    }

    @Bean
    PlatformIndicatorProperties platformIndicatorProperties(
            @Value("${copilot.platform.indicator.base-url:}") String baseUrl,
            @Value("${copilot.platform.indicator.token-url:}") String tokenUrl,
            @Value("${copilot.platform.indicator.client-id:}") String clientId,
            @Value("${copilot.platform.indicator.client-secret:}") String clientSecret,
            @Value("${copilot.platform.indicator.auth-token:}") String authToken,
            @Value("${copilot.platform.indicator.service-name:dts-copilot}") String serviceName,
            @Value("${copilot.platform.indicator.service-token:${DTS_INBOUND_FROM_COPILOT:${DTS_INBOUND_FROM_ANALYTICS:${DTS_ADMIN_SERVICE_TOKEN:}}}}") String serviceToken,
            @Value("${copilot.platform.indicator.timeout-seconds:10}") int timeoutSeconds) {
        return new PlatformIndicatorProperties(
                baseUrl,
                tokenUrl,
                clientId,
                clientSecret,
                authToken,
                serviceName,
                serviceToken,
                timeoutSeconds);
    }
}
