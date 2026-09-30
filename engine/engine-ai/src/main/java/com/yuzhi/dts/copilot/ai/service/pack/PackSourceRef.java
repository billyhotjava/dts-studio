package com.yuzhi.dts.copilot.ai.service.pack;

/** Identifies an immutable installed Pack version used by an execution. */
public record PackSourceRef(String type, String name, String version) {
    public PackSourceRef {
        if (!"pack".equals(type) || name == null || name.isBlank() || version == null || version.isBlank()) {
            throw new IllegalArgumentException("A Pack source requires its name and version");
        }
    }

    public PackSourceRef(String name, String version) {
        this("pack", name, version);
    }

    public static PackSourceRef from(PackAssetResolver.Asset asset) {
        return new PackSourceRef(asset.packName(), asset.packVersion());
    }
}
