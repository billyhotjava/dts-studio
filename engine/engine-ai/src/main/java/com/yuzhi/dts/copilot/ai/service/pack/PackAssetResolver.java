package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;

public interface PackAssetResolver {
    record Asset(String packName, String packVersion, String kind, String key, String text, JsonNode json) {}
    record Snapshot(long generation, List<Asset> assets) {
        public Snapshot { assets = List.copyOf(assets); }
    }
    Snapshot snapshot();
    default long generation() { return snapshot().generation(); }
    default List<Asset> list(String kind) {
        return snapshot().assets().stream().filter(a -> a.kind().equals(kind)).toList();
    }
    default Optional<Asset> resolve(String kind, String key) {
        return list(kind).stream().filter(a -> a.key().equals(key)).findFirst();
    }
}
