package com.yuzhi.dts.copilot.ai.service.pack;

import org.springframework.beans.factory.annotation.Autowired;
import java.io.InputStream;

/** Readers lock their public methods so a generation is fully loaded before publication. */
public abstract class PackBackedJsonRegistry {
    private PackResourceReader resources = PackResourceReader.legacyForStandaloneTests();
    private boolean managed;
    private long loadedGeneration = Long.MIN_VALUE;
    private PackAssetResolver.Snapshot loadingSnapshot;
    private java.util.List<PackSourceRef> loadedSources = java.util.List.of();
    private java.util.Set<PackSourceRef> loadingSources;

    @Autowired
    public final synchronized void setPackResourceReader(PackResourceReader reader) {
        resources = reader;
        managed = true;
        loadedGeneration = Long.MIN_VALUE;
    }

    protected final boolean hasManagedPackReader() { return managed; }
    protected final PackResourceReader packResources() { return resources; }

    protected final synchronized void ensurePackResources(Runnable load) {
        var snapshot = resources.snapshot();
        if (loadedGeneration == snapshot.generation()) {
            PackReadScope.record(loadedSources);
            return;
        }
        loadingSnapshot = snapshot;
        loadingSources = new java.util.LinkedHashSet<>();
        try {
            load.run();
            loadedGeneration = snapshot.generation();
            loadedSources = java.util.List.copyOf(loadingSources);
            PackReadScope.record(loadedSources);
        } finally {
            loadingSnapshot = null;
            loadingSources = null;
        }
    }

    protected final PackAssetResolver.Snapshot packSnapshot() {
        return loadingSnapshot == null ? resources.snapshot() : loadingSnapshot;
    }

    protected final InputStream openPackResource(String resource) {
        var snapshot = packSnapshot();
        resources.resolve(resource, snapshot).ifPresent(this::usePackAsset);
        return resources.open(resource, snapshot);
    }

    /** Retains the dependency alongside a parsed cache so cache hits remain traceable. */
    protected final void usePackAsset(PackAssetResolver.Asset asset) {
        var source = PackSourceRef.from(asset);
        if (loadingSources != null) loadingSources.add(source);
        PackReadScope.record(java.util.List.of(source));
    }
}
