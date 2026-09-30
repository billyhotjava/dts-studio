package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface ActionClient {
    ActionResponse invoke(Target target, Map<String, Object> payload, Caller caller);

    record Target(String serviceRef, String method, String path) {}
    record Caller(String actorId, String requestId) {}
    record ActionResponse(boolean success, String message, Map<String, Object> body) {
        public ActionResponse {
            body = body == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(body));
        }
    }
}
