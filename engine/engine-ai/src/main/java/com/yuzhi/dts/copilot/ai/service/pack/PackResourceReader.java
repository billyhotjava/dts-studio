package com.yuzhi.dts.copilot.ai.service.pack;

import com.yuzhi.dts.common.pack.PackException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Transitional resource names are stable asset keys, shared by every legacy reader. */
@Component
public class PackResourceReader {
    private final PackAssetResolver resolver;
    private final boolean legacyFallback;

    public PackResourceReader(PackAssetResolver resolver,
            @Value("${dts.studio.pack.fallback-classpath:true}") boolean legacyFallback) {
        this.resolver = resolver;
        this.legacyFallback = legacyFallback;
    }

    public static PackResourceReader legacyForStandaloneTests() {
        return new PackResourceReader(() -> new PackAssetResolver.Snapshot(-1, List.of()), true);
    }

    public PackAssetResolver.Snapshot snapshot() { return resolver.snapshot(); }
    public boolean useLegacy(PackAssetResolver.Snapshot snapshot) {
        return legacyFallback && snapshot.generation() <= 0;
    }

    public java.util.Optional<PackAssetResolver.Asset> resolve(String resource, PackAssetResolver.Snapshot snapshot) {
        String key = resource.startsWith("/") ? resource.substring(1) : resource;
        PackArchiveValidator.requireSafePath(key);
        return snapshot.assets().stream().filter(a -> a.key().equals(key) && a.kind().equals(kind(key))).findFirst();
    }

    public InputStream open(String resource, PackAssetResolver.Snapshot snapshot) {
        var asset = resolve(resource, snapshot);
        if (asset.isPresent()) {
            var value = asset.get();
            PackReadScope.record(List.of(PackSourceRef.from(value)));
            String text = value.json() == null ? value.text() : value.json().toString();
            return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        }
        String key = resource.startsWith("/") ? resource.substring(1) : resource;
        if (useLegacy(snapshot)) return getClass().getClassLoader().getResourceAsStream(key);
        throw new PackException(503, "PACK_ASSET_UNAVAILABLE", "Required pack asset is not active: " + key);
    }

    public static String kind(String key) {
        if (key.startsWith("semantic-packs/")) return "ontology";
        if (key.startsWith("prompts/")) return "prompts";
        if (key.startsWith("planner/")) return "persona";
        if (key.startsWith("governance/")) {
            if (key.contains("caliber-rules")) return "guardrails";
            if (key.contains("regression") || key.contains("golden") || key.contains("cases") || key.contains("samples")) return "evaluations";
            return "quality_rules";
        }
        throw new IllegalArgumentException("Unsupported legacy asset resource: " + key);
    }
}
