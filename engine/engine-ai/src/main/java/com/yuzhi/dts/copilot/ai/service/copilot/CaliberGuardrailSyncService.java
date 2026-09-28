package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CaliberGuardrailSyncService {

    private final GovernanceCaliberExportProvider provider;
    private final SemanticPackService semanticPackService;
    private final ObjectMapper objectMapper;
    private volatile GovernanceCaliberExport lastSuccessfulExport;

    public CaliberGuardrailSyncService(
            GovernanceCaliberExportProvider provider,
            SemanticPackService semanticPackService,
            ObjectMapper objectMapper) {
        this.provider = provider;
        this.semanticPackService = semanticPackService;
        this.objectMapper = objectMapper;
    }

    public SyncReport refresh() {
        try {
            GovernanceCaliberExport export = provider.fetch();
            lastSuccessfulExport = export;
            return buildReport(SyncStatus.SUCCESS, false, FallbackMode.NONE, export, null);
        } catch (RuntimeException e) {
            String error = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            GovernanceCaliberExport cached = lastSuccessfulExport;
            if (cached != null) {
                return buildReport(SyncStatus.FAILED, true, FallbackMode.STALE_CACHE, cached, error);
            }
            return buildStaticPackFallbackReport(error);
        }
    }

    private SyncReport buildReport(
            SyncStatus status,
            boolean stale,
            FallbackMode fallbackMode,
            GovernanceCaliberExport export,
            String error) {
        Map<String, List<String>> guardrailsByDomain = export.guardrailsByDomain();
        List<SyncDrift> drifts = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : guardrailsByDomain.entrySet()) {
            drifts.addAll(compareGeneratedPack(entry.getKey(), entry.getValue()));
        }
        return new SyncReport(
                status,
                stale,
                fallbackMode,
                export.version(),
                export.contentHash(),
                guardrailsByDomain,
                !drifts.isEmpty(),
                drifts,
                Instant.now(),
                error);
    }

    private SyncReport buildStaticPackFallbackReport(String error) {
        Map<String, List<String>> fallbackGuardrails = new LinkedHashMap<>();
        for (String domain : semanticPackService.getDomains()) {
            semanticPackService.getPack(domain)
                    .map(SemanticPackService.SemanticPack::guardrails)
                    .filter(guardrails -> !guardrails.isEmpty())
                    .ifPresent(guardrails -> fallbackGuardrails.put(domain, guardrails));
        }
        return new SyncReport(
                SyncStatus.FAILED,
                true,
                FallbackMode.STATIC_PACK,
                "",
                "",
                fallbackGuardrails,
                false,
                List.of(),
                Instant.now(),
                error);
    }

    private List<SyncDrift> compareGeneratedPack(String domain, List<String> expectedGuardrails) {
        Map<String, String> expectedByRule = byRuleId(expectedGuardrails);
        Map<String, String> generatedByRule = byRuleId(readGeneratedGuardrails(domain));
        List<SyncDrift> drifts = new ArrayList<>();
        for (Map.Entry<String, String> expected : expectedByRule.entrySet()) {
            String actual = generatedByRule.get(expected.getKey());
            if (actual == null) {
                drifts.add(new SyncDrift(domain, expected.getKey(), "MISSING_IN_PACK",
                        "pack generatedGuardrails is missing governance rule"));
            } else if (!actual.equals(expected.getValue())) {
                drifts.add(new SyncDrift(domain, expected.getKey(), "GUARDRAIL_TEXT_MISMATCH",
                        "pack generatedGuardrails differs from governance export"));
            }
        }
        for (String ruleId : generatedByRule.keySet()) {
            if (!expectedByRule.containsKey(ruleId)) {
                drifts.add(new SyncDrift(domain, ruleId, "EXTRA_IN_PACK",
                        "pack generatedGuardrails contains a rule absent from governance export"));
            }
        }
        return drifts;
    }

    private List<String> readGeneratedGuardrails(String domain) {
        String resource = "semantic-packs/" + domain + ".json";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                return List.of();
            }
            JsonNode rules = objectMapper.readTree(is).path("generatedGuardrails").path("rules");
            if (!rules.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonNode rule : rules) {
                String value = rule.asText("");
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
            return List.copyOf(values);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static Map<String, String> byRuleId(List<String> guardrails) {
        Map<String, String> byRule = new LinkedHashMap<>();
        for (String guardrail : guardrails) {
            String ruleId = extractRuleId(guardrail);
            if (!ruleId.isBlank()) {
                byRule.put(ruleId, guardrail);
            }
        }
        return byRule;
    }

    private static String extractRuleId(String value) {
        if (value == null || !value.startsWith("[CAL-")) {
            return "";
        }
        int end = value.indexOf(']');
        return end <= 1 ? "" : value.substring(1, end);
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static Map<String, List<String>> copyMapOfLists(Map<String, List<String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            if (StringUtils.hasText(entry.getKey())) {
                copied.put(normalizeDomainKey(entry.getKey()), copyOrEmpty(entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(copied);
    }

    public interface GovernanceCaliberExportProvider {
        GovernanceCaliberExport fetch();

        default GovernanceCaliberExport fetch(List<String> domains) {
            GovernanceCaliberExport export = fetch();
            if (domains == null || domains.isEmpty()) {
                return export;
            }
            Map<String, List<String>> selected = new LinkedHashMap<>();
            for (String domain : domains) {
                String key = normalizeDomainKey(domain);
                List<String> guardrails = export.guardrailsByDomain().get(key);
                if (guardrails != null) {
                    selected.put(key, guardrails);
                }
            }
            return new GovernanceCaliberExport(export.version(), selected);
        }
    }

    public record GovernanceCaliberExport(String version, String contentHash, Map<String, List<String>> guardrailsByDomain) {

        public GovernanceCaliberExport(String version, Map<String, List<String>> guardrailsByDomain) {
            this(version, "", guardrailsByDomain);
        }

        public GovernanceCaliberExport {
            version = version == null ? "" : version;
            guardrailsByDomain = copyMapOfLists(guardrailsByDomain);
            contentHash = StringUtils.hasText(contentHash)
                    ? contentHash
                    : stableContentHash(version, guardrailsByDomain);
        }

        public List<String> guardrailsForDomain(String domain) {
            if (!StringUtils.hasText(domain)) {
                return List.of();
            }
            return guardrailsByDomain.getOrDefault(normalizeDomainKey(domain), List.of());
        }
    }

    public enum SyncStatus {
        SUCCESS,
        FAILED
    }

    public enum FallbackMode {
        NONE,
        STALE_CACHE,
        STATIC_PACK
    }

    public record SyncReport(
            SyncStatus status,
            boolean stale,
            FallbackMode fallbackMode,
            String exportVersion,
            String exportHash,
            Map<String, List<String>> guardrailsByDomain,
            boolean drifted,
            List<SyncDrift> drifts,
            Instant completedAt,
            String error) {
        public SyncReport {
            status = status == null ? SyncStatus.FAILED : status;
            fallbackMode = fallbackMode == null ? FallbackMode.NONE : fallbackMode;
            exportVersion = exportVersion == null ? "" : exportVersion;
            exportHash = exportHash == null ? "" : exportHash;
            guardrailsByDomain = copyMapOfLists(guardrailsByDomain);
            drifts = copyOrEmpty(drifts);
            completedAt = completedAt == null ? Instant.now() : completedAt;
        }

        public List<String> guardrailsForDomain(String domain) {
            if (!StringUtils.hasText(domain)) {
                return List.of();
            }
            return guardrailsByDomain.getOrDefault(normalizeDomainKey(domain), List.of());
        }
    }

    public record SyncDrift(String domain, String ruleId, String type, String message) {
        public SyncDrift {
            domain = domain == null ? "" : domain;
            ruleId = ruleId == null ? "" : ruleId;
            type = type == null ? "" : type;
            message = message == null ? "" : message;
        }
    }

    private static String stableContentHash(String version, Map<String, List<String>> guardrailsByDomain) {
        StringBuilder canonical = new StringBuilder();
        canonical.append(version == null ? "" : version).append('\n');
        Map<String, List<String>> sorted = new TreeMap<>(guardrailsByDomain == null ? Map.of() : guardrailsByDomain);
        for (Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            canonical.append(entry.getKey()).append('\n');
            for (String guardrail : entry.getValue()) {
                canonical.append(guardrail == null ? "" : guardrail).append('\n');
            }
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(
                    digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private static String normalizeDomainKey(String domain) {
        return domain == null ? "" : domain.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
