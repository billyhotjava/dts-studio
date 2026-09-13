package com.yuzhi.dts.copilot.ai.service.platform;

import java.util.Collection;
import java.util.Map;

public interface DbtFreshnessResolver {

    Map<String, String> resolveFreshness(Collection<String> relations);
}
