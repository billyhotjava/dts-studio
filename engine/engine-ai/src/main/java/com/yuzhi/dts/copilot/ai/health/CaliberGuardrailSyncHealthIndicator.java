package com.yuzhi.dts.copilot.ai.health;

import com.yuzhi.dts.copilot.ai.service.copilot.CaliberGuardrailSyncService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CaliberGuardrailSyncHealthIndicator implements HealthIndicator {

    private final CaliberGuardrailSyncService syncService;

    public CaliberGuardrailSyncHealthIndicator(CaliberGuardrailSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public Health health() {
        CaliberGuardrailSyncService.SyncReport report = syncService.refresh();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lastStatus", report.status().name());
        details.put("caliberSyncDrift", report.drifted());
        details.put("stale", report.stale());
        details.put("fallbackMode", report.fallbackMode().name());
        details.put("exportVersion", report.exportVersion());
        details.put("exportHash", report.exportHash());
        details.put("domainCount", report.guardrailsByDomain().size());
        details.put("driftCount", report.drifts().size());
        details.put("drifts", driftDetails(report.drifts()));
        details.put("error", report.error());
        details.put("completedAt", report.completedAt());
        return Health.up().withDetails(details).build();
    }

    private static List<Map<String, String>> driftDetails(List<CaliberGuardrailSyncService.SyncDrift> drifts) {
        return drifts.stream()
                .map(drift -> {
                    Map<String, String> detail = new LinkedHashMap<>();
                    detail.put("domain", drift.domain());
                    detail.put("ruleId", drift.ruleId());
                    detail.put("type", drift.type());
                    detail.put("message", drift.message());
                    return detail;
                })
                .toList();
    }
}
