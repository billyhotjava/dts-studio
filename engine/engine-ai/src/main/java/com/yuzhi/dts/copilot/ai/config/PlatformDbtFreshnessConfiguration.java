package com.yuzhi.dts.copilot.ai.config;

import com.yuzhi.dts.copilot.ai.service.platform.PlatformDbtFreshnessProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlatformDbtFreshnessConfiguration {

    @Bean
    PlatformDbtFreshnessProperties platformDbtFreshnessProperties(
            @Value("${copilot.platform.dbt-freshness.enabled:true}") boolean enabled,
            @Value("${copilot.platform.dbt-freshness.base-url:${copilot.platform.indicator.base-url:}}") String baseUrl,
            @Value("${copilot.platform.dbt-freshness.auth-token:${copilot.platform.indicator.auth-token:}}") String authToken,
            @Value("${copilot.platform.dbt-freshness.service-name:${copilot.platform.indicator.service-name:dts-copilot}}") String serviceName,
            @Value("${copilot.platform.dbt-freshness.service-token:${copilot.platform.indicator.service-token:${DTS_PLATFORM_SERVICE_TOKEN:${DTS_INBOUND_FROM_COPILOT:${DTS_INBOUND_FROM_ANALYTICS:${DTS_ADMIN_SERVICE_TOKEN:}}}}}}") String serviceToken,
            @Value("${copilot.platform.dbt-freshness.active-dept:${copilot.platform.indicator.active-dept:}}") String activeDept,
            @Value("${copilot.platform.dbt-freshness.timeout-seconds:${copilot.platform.indicator.timeout-seconds:10}}") int timeoutSeconds,
            @Value("${copilot.platform.dbt-freshness.stale-after-hours:24}") int staleAfterHours) {
        return new PlatformDbtFreshnessProperties(
                enabled,
                baseUrl,
                authToken,
                serviceName,
                serviceToken,
                activeDept,
                timeoutSeconds,
                staleAfterHours);
    }
}
