package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.security.ApiKeyAuthentication;
import com.yuzhi.dts.copilot.ai.service.pack.PackArchiveValidator;
import com.yuzhi.dts.copilot.ai.service.pack.PackException;
import com.yuzhi.dts.copilot.ai.service.pack.PackRegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/** Platform-wide pack administration. Caller-provided roles do not authorize this API. */
@RestController
@RequestMapping("/api/ai/packs")
public class PackResource {
    private final PackRegistryService registry;
    private final PackArchiveValidator validator;
    private final String adminSecret;

    public PackResource(PackRegistryService registry, PackArchiveValidator validator,
                        @Value("${copilot.admin-secret:}") String adminSecret) {
        this.registry = registry;
        this.validator = validator;
        this.adminSecret = adminSecret;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> install(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                     @RequestPart("file") MultipartFile file) throws IOException {
        String actor = administrator(secret);
        if (file.getSize() > PackArchiveValidator.MAX_ARCHIVE)
            throw new PackException(413, "PACK_TOO_LARGE", "Pack exceeds upload limit");
        PackRegistryService.Installed installed;
        try (var input = file.getInputStream()) {
            installed = registry.install(validator.validate(input), actor);
        }
        var version = installed.version();
        return ResponseEntity.status(installed.created() ? 201 : 200).body(Map.of(
                "packId", version.packId(), "versionId", version.versionId(), "name", version.name(),
                "version", version.version(), "status", version.status(),
                "validation", Map.of("errors", List.of(), "warnings", installed.warnings())));
    }

    @GetMapping
    public List<PackRegistryService.Version> list(@RequestHeader(value = "X-Admin-Secret", required = false) String secret) {
        administrator(secret);
        return registry.versions(null, null);
    }

    @GetMapping("/{name}")
    public List<PackRegistryService.Version> detail(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                                   @PathVariable String name) {
        administrator(secret);
        var versions = registry.versions(name, null);
        if (versions.isEmpty()) throw new PackException(404, "PACK_NOT_FOUND", "Pack not found");
        return versions;
    }

    @PostMapping("/{name}/versions/{version}/activate")
    public PackRegistryService.Activated activate(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                                  @PathVariable String name, @PathVariable String version) {
        return registry.activate(name, version, administrator(secret));
    }

    @PostMapping("/{name}/rollback")
    public PackRegistryService.Activated rollback(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                                  @PathVariable String name) {
        return registry.rollback(name, administrator(secret));
    }

    @GetMapping("/{name}/versions/{version}/assets")
    public List<Map<String, Object>> assets(@RequestHeader(value = "X-Admin-Secret", required = false) String secret,
                                            @PathVariable String name, @PathVariable String version,
                                            @RequestParam(required = false) String kind) {
        administrator(secret);
        return registry.assets(name, version, kind);
    }

    private String administrator(String provided) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof ApiKeyAuthentication apiKey) || !authentication.isAuthenticated()
                || adminSecret == null || adminSecret.isBlank() || adminSecret.equals("change-me-in-production")
                || provided == null || !MessageDigest.isEqual(adminSecret.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            throw new PackException(403, "PACK_FORBIDDEN", "Authenticated pack administrator required");
        }
        return "api-key:" + apiKey.getApiKey().getId();
    }

    @ExceptionHandler(PackException.class)
    public ResponseEntity<?> packError(PackException error) {
        return ResponseEntity.status(error.status()).body(Map.of("code", error.code(), "errors", error.errors()));
    }
}
