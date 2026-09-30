package com.yuzhi.dts.copilot.ai.service.pack;

import org.springframework.boot.test.context.SpringBootTest;

/** Explicit legacy-compatible baseline lane; requires disposable PostgreSQL with pgvector. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "copilot.admin-secret=isolated-pack-smoke-secret", "dts.studio.pack.fallback-classpath=false",
        "copilot.platform.indicator.sync.enabled=false", "dts.copilot.ai.config-path=/nonexistent/studio-test-config.json"})
class PackRuntimeSmokeIT extends PackRuntimeTestSupport {
}
