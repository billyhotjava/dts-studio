package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.Collections;
import java.util.Map;

public interface AdminApiActionClient {

    AdminApiActionResponse postDraft(String endpoint, Map<String, Object> payload);

    AdminApiActionResponse postCommit(String endpoint, Map<String, Object> payload);

    record AdminApiActionResponse(
            boolean success,
            String message,
            Map<String, Object> body) {
        public AdminApiActionResponse {
            body = body == null ? Map.of() : Collections.unmodifiableMap(body);
        }
    }
}
