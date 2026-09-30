package com.yuzhi.dts.copilot.ai.service.pack;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Proves the actual fresh-install profile, without a test override for classpath fallback. */
@ActiveProfiles("studio-pack")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "copilot.admin-secret=isolated-pack-smoke-secret", "copilot.platform.indicator.sync.enabled=false",
        "dts.copilot.ai.config-path=/nonexistent/studio-test-config.json"})
class PackFreshBootstrapSmokeIT extends PackRuntimeTestSupport {
    @Override protected boolean freshBootstrap() { return true; }
}
