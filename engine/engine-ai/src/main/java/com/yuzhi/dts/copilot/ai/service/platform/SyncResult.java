package com.yuzhi.dts.copilot.ai.service.platform;

import java.time.Instant;
import java.util.List;

public record SyncResult(
        Status status,
        int fetched,
        int added,
        int updated,
        int removed,
        List<String> caliberChangedCodes,
        Instant completedAt,
        String error
) {
    public SyncResult {
        status = status == null ? Status.FAILED : status;
        fetched = Math.max(0, fetched);
        added = Math.max(0, added);
        updated = Math.max(0, updated);
        removed = Math.max(0, removed);
        caliberChangedCodes = caliberChangedCodes == null ? List.of() : List.copyOf(caliberChangedCodes);
        completedAt = completedAt == null ? Instant.now() : completedAt;
    }

    public static SyncResult success(
            int fetched,
            int added,
            int updated,
            int removed,
            List<String> caliberChangedCodes) {
        return new SyncResult(Status.SUCCESS, fetched, added, updated, removed, caliberChangedCodes, Instant.now(), null);
    }

    public static SyncResult failed(int fetched, String error) {
        return new SyncResult(Status.FAILED, fetched, 0, 0, 0, List.of(), Instant.now(), error);
    }

    public static SyncResult skipped(String reason) {
        return new SyncResult(Status.SKIPPED, 0, 0, 0, 0, List.of(), Instant.now(), reason);
    }

    public enum Status {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
