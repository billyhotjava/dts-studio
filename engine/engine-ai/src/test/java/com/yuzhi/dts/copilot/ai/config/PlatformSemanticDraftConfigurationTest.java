package com.yuzhi.dts.copilot.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.copilot.ai.service.platform.PlatformSemanticDraftProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlatformSemanticDraftConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PlatformSemanticDraftConfiguration.class);

    @Test
    void shouldInheritIndicatorPlatformConnectionSettingsByDefault() {
        contextRunner
                .withPropertyValues(
                        "copilot.platform.indicator.base-url=http://dts-platform:8081",
                        "copilot.platform.indicator.auth-token=platform-auth-token",
                        "copilot.platform.indicator.service-name=dts-copilot-live",
                        "copilot.platform.indicator.service-token=platform-service-token",
                        "copilot.platform.indicator.timeout-seconds=7")
                .run(context -> {
                    PlatformSemanticDraftProperties properties =
                            context.getBean(PlatformSemanticDraftProperties.class);

                    assertThat(properties.baseUrl()).isEqualTo("http://dts-platform:8081");
                    assertThat(properties.authToken()).isEqualTo("platform-auth-token");
                    assertThat(properties.serviceName()).isEqualTo("dts-copilot-live");
                    assertThat(properties.serviceToken()).isEqualTo("platform-service-token");
                    assertThat(properties.timeoutSeconds()).isEqualTo(7);
                });
    }
}
