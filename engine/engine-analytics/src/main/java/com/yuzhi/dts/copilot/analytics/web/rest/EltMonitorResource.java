package com.yuzhi.dts.copilot.analytics.web.rest;

import com.yuzhi.dts.copilot.analytics.domain.EltSyncWatermark;
import com.yuzhi.dts.copilot.analytics.repository.EltSyncWatermarkRepository;
import com.yuzhi.dts.copilot.analytics.service.elt.EltSyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics/elt")
@ConditionalOnProperty(name = "dts.elt.enabled", havingValue = "true")
public class EltMonitorResource {

    private static final Logger log = LoggerFactory.getLogger(EltMonitorResource.class);

    private final EltSyncWatermarkRepository watermarkRepository;
    private final List<EltSyncJob> syncJobs;

    public EltMonitorResource(EltSyncWatermarkRepository watermarkRepository,
                              List<EltSyncJob> syncJobs) {
        this.watermarkRepository = watermarkRepository;
        this.syncJobs = syncJobs;
    }

    public record EltStatusResponse(
            String targetTable,
            String syncStatus,
            Instant lastSyncTime,
            Integer lastSyncRows,
            Long lastSyncDurationMs,
            Long syncDelayMinutes,
            boolean isHealthy
    ) {}

    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EltStatusResponse>> listStatus() {
        List<EltSyncWatermark> watermarks = watermarkRepository.findAll();
        List<EltStatusResponse> responses = watermarks.stream()
                .map(this::toStatusResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping(path = "/trigger/{table}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> triggerSync(@PathVariable String table) {
        Optional<EltSyncJob> job = syncJobs.stream()
                .filter(j -> table.equals(j.getTargetTable()))
                .findFirst();
        if (job.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No sync job found for table: " + table));
        }

        String batchId = UUID.randomUUID().toString();
        CompletableFuture.runAsync(() -> {
            try {
                Optional<EltSyncWatermark> wm = watermarkRepository.findByTargetTable(table);
                Instant lastWatermark = wm.map(EltSyncWatermark::getLastWatermark).orElse(Instant.EPOCH);
                job.get().sync(lastWatermark, batchId);
            } catch (Exception e) {
                log.error("Triggered sync failed for table {}: {}", table, e.getMessage(), e);
            }
        });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("table", table);
        body.put("batchId", batchId);
        body.put("status", "TRIGGERED");
        return ResponseEntity.accepted().body(body);
    }

    @PostMapping(path = "/reset/{table}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> resetWatermark(@PathVariable String table) {
        Optional<EltSyncWatermark> wm = watermarkRepository.findByTargetTable(table);
        if (wm.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No watermark found for table: " + table));
        }

        EltSyncWatermark watermark = wm.get();
        watermark.setLastWatermark(Instant.EPOCH);
        watermark.setStatus("RESET");
        watermark.setErrorMessage(null);
        watermarkRepository.save(watermark);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("table", table);
        body.put("status", "RESET");
        body.put("lastWatermark", Instant.EPOCH.toString());
        return ResponseEntity.ok(body);
    }

    private EltStatusResponse toStatusResponse(EltSyncWatermark wm) {
        long delayMinutes = Duration.between(wm.getUpdatedAt(), Instant.now()).toMinutes();
        boolean healthy = !"FAILED".equals(wm.getStatus()) && delayMinutes <= 120;
        Long durationMs = wm.getLastWatermark() != null && wm.getUpdatedAt() != null
                ? Duration.between(wm.getLastWatermark(), wm.getUpdatedAt()).toMillis()
                : null;
        return new EltStatusResponse(
                wm.getTargetTable(),
                wm.getStatus(),
                wm.getUpdatedAt(),
                wm.getLastRowCount(),
                durationMs,
                delayMinutes,
                healthy
        );
    }
}
