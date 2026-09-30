package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Shared, offline-only validation for CLI and HTTP installation. No filesystem extraction. */
@Component
public class PackArchiveValidator {
    public static final int MAX_ARCHIVE = 20 * 1024 * 1024;
    public static final int MAX_ENTRY = 8 * 1024 * 1024;
    public static final int MAX_EXPANDED = 64 * 1024 * 1024;
    private static final String ENGINE_VERSION = "1.0.0";
    private static final Map<String, String> SCHEMAS = Map.of(
            "ontology", "ontology", "guardrails", "guardrail", "quality_rules", "quality-rule",
            "templates", "template", "actions", "action", "evaluations", "evaluation");
    private static final Set<String> KINDS = Set.of("ontology", "metrics", "actions", "skills",
            "guardrails", "quality_rules", "templates", "prompts", "persona", "evaluations");
    private final ObjectMapper yaml;

    public PackArchiveValidator() {
        YAMLFactory factory = new YAMLFactory();
        factory.setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(64).maxStringLength(MAX_ENTRY).build());
        yaml = new ObjectMapper(factory).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    public record Asset(String kind, String key, String path, String text, JsonNode json, String sha256) {}
    public record ValidatedPack(JsonNode manifest, String checksum, List<Asset> assets, List<String> warnings) {
        public String name() { return manifest.path("name").asText(); }
        public String version() { return manifest.path("version").asText(); }
        public String vendor() { return manifest.path("vendor").asText(); }
    }

    public ValidatedPack validate(InputStream stream) {
        try {
            byte[] archive = stream.readNBytes(MAX_ARCHIVE + 1);
            if (archive.length > MAX_ARCHIVE) throw tooLarge();
            Map<String, byte[]> files = unzip(archive);
            byte[] manifestBytes = files.get("pack-manifest.yaml");
            if (manifestBytes == null) throw invalid("Missing pack-manifest.yaml");
            JsonNode manifest = yaml.readTree(manifestBytes);
            validateSchema("pack-manifest.v1.schema.json", manifest);
            checkVersion(manifest.path("requires").path("dts-studio").asText());
            verifyHashes(files);
            List<Asset> assets = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            Set<String> keys = new HashSet<>();
            Set<String> referenced = new HashSet<>(Set.of("pack-manifest.yaml", "SHA256SUMS"));
            var capabilities = manifest.path("capabilities").fields();
            while (capabilities.hasNext()) {
                var capability = capabilities.next();
                String kind = capability.getKey();
                if (!KINDS.contains(kind)) warnings.add("Unknown capability: " + kind);
                for (JsonNode entry : capability.getValue()) {
                    String path = entry.path("path").asText();
                    requireSafePath(path);
                    byte[] bytes = files.get(path);
                    if (bytes == null) throw invalid("Missing asset: " + path);
                    referenced.add(path);
                    String key = entry.path("key").asText(path);
                    if (key.length() > 128 || !keys.add(kind + ":" + key)) throw invalid("Duplicate or long asset key");
                    String text = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                    JsonNode json = kind.equals("prompts") || !KINDS.contains(kind) ? null : yaml.readTree(bytes);
                    if (json == null && KINDS.contains(kind) && !kind.equals("prompts")) throw invalid("Empty structured asset: " + path);
                    if (SCHEMAS.containsKey(kind)) validateSchema("assets/" + SCHEMAS.get(kind) + ".v1.schema.json", json);
                    if (kind.equals("ontology")) checkOntology(json);
                    if (kind.equals("metrics")) checkMetricReferences(json);
                    if (KINDS.contains(kind)) assets.add(new Asset(kind, key, path, text, json, sha256(bytes)));
                }
            }
            if (!referenced.equals(files.keySet())) throw invalid("Archive contains undeclared files");
            if (assets.isEmpty()) throw invalid("Pack has no supported assets");
            Set<String> templateIds = new HashSet<>();
            Set<String> datasourceRefs = new HashSet<>();
            manifest.path("datasources").forEach(ref -> datasourceRefs.add(ref.path("ref").asText()));
            for (Asset asset : assets) {
                if (asset.kind().equals("templates")) {
                    for (JsonNode template : asset.json().path("templates")) {
                        if (!templateIds.add(template.path("id").asText())) throw invalid("Duplicate template ID");
                        if (!datasourceRefs.contains(template.path("datasource_ref").asText()))
                            throw invalid("Template refers to an undeclared datasource");
                        try {
                            for (JsonNode pattern : template.path("question_patterns"))
                                java.util.regex.Pattern.compile(pattern.asText());
                        } catch (java.util.regex.PatternSyntaxException e) { throw invalid("Invalid template question pattern"); }
                    }
                }
            }
            Set<String> domains = new HashSet<>();
            for (Asset asset : assets) {
                if (asset.kind().equals("ontology")) {
                    if (!domains.add(asset.json().path("domain").asText())) throw invalid("Duplicate ontology domain");
                }
            }
            Map<String, Set<String>> actionsByDomain = new LinkedHashMap<>();
            for (Asset asset : assets) {
                if (!asset.kind().equals("ontology")) continue;
                var names = new HashSet<String>();
                for (JsonNode action : asset.json().path("actions")) {
                    if (!names.add(action.path("name").asText())) throw invalid("Duplicate ontology action");
                    if (action.has("target")) validateSchema("assets/action.v1.schema.json", action);
                }
                actionsByDomain.put(asset.json().path("domain").asText(), names);
            }
            for (Asset asset : assets) {
                if (!asset.kind().equals("actions")) continue;
                JsonNode action = asset.json();
                List<String> owners = new ArrayList<>();
                for (Asset ontology : assets) {
                    if (!ontology.kind().equals("ontology")) continue;
                    String domain = ontology.json().path("domain").asText();
                    if (action.hasNonNull("domain") && !action.path("domain").asText().equals(domain)) continue;
                    for (JsonNode object : ontology.json().path("objects")) {
                        if (object.path("name").asText().equals(action.path("object").asText())) {
                            owners.add(domain); break;
                        }
                    }
                }
                if (owners.size() != 1) throw invalid("Action requires exactly one owning ontology");
                if (!actionsByDomain.get(owners.getFirst()).add(action.path("name").asText())) throw invalid("Duplicate ontology action");
            }
            for (Asset asset : assets) {
                if (!asset.kind().equals("ontology")) continue;
                Set<String> known = actionsByDomain.get(asset.json().path("domain").asText());
                for (JsonNode signal : asset.json().path("signals")) {
                    for (JsonNode action : signal.path("linkedActions")) {
                        if (!known.contains(action.asText())) throw invalid("Signal refers to an unknown action");
                    }
                }
            }
            return new ValidatedPack(manifest, sha256(archive), List.copyOf(assets), List.copyOf(warnings));
        } catch (PackException e) {
            throw e;
        } catch (IOException | IllegalArgumentException e) {
            throw invalid("Malformed archive or asset document");
        }
    }

    private Map<String, byte[]> unzip(byte[] archive) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        int count = 0;
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > 2000) throw tooLarge();
                String name = entry.getName();
                requireSafePath(entry.isDirectory() && name.endsWith("/") ? name.substring(0, name.length() - 1) : name);
                if (entry.isDirectory()) continue;
                if (files.containsKey(name)) throw invalid("Duplicate archive entry");
                byte[] bytes = zip.readNBytes(MAX_ENTRY + 1);
                total += bytes.length;
                if (bytes.length > MAX_ENTRY || total > MAX_EXPANDED) throw tooLarge();
                files.put(name, bytes);
            }
        }
        return files;
    }

    public static void requireSafePath(String path) {
        if (path.isBlank() || path.startsWith("/") || path.contains("\\") || path.contains(":")
                || path.chars().anyMatch(c -> c < 32)) throw invalid("Unsafe archive path");
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) throw invalid("Unsafe archive path");
        }
    }

    private void verifyHashes(Map<String, byte[]> files) {
        byte[] sums = files.get("SHA256SUMS");
        if (sums == null) throw invalid("Missing SHA256SUMS");
        Set<String> seen = new HashSet<>();
        for (String line : new String(sums, StandardCharsets.UTF_8).split("\\R")) {
            if (line.isBlank()) continue;
            if (line.length() < 67 || !line.substring(0, 64).matches("[a-f0-9]{64}")
                    || !line.substring(64, 66).equals("  ")) throw invalid("Invalid SHA256SUMS entry");
            String path = line.substring(66);
            requireSafePath(path);
            byte[] data = files.get(path);
            if (path.equals("SHA256SUMS") || !seen.add(path) || data == null
                    || !sha256(data).equals(line.substring(0, 64))) throw invalid("Checksum mismatch: " + path);
        }
        if (seen.size() != files.size() - 1) throw invalid("Every file must have a checksum");
    }

    public void validateSchema(String name, JsonNode value) throws IOException {
        try (InputStream schema = getClass().getResourceAsStream("/protocol/" + name)) {
            if (schema == null) throw new IllegalStateException("Missing bundled schema: " + name);
            var errors = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(schema).validate(value);
            if (!errors.isEmpty()) throw new PackException(422, "PACK_INVALID",
                    errors.stream().map(Object::toString).sorted().toList());
        }
    }

    private void checkVersion(String requirement) {
        if (!requirement.startsWith(">=")) throw invalid("Unsupported engine version requirement");
        String[] required = requirement.substring(2).split("\\.");
        String[] current = ENGINE_VERSION.split("\\.");
        for (int i = 0; i < 3; i++) {
            int compare = new java.math.BigInteger(current[i]).compareTo(new java.math.BigInteger(required[i]));
            if (compare > 0) return;
            if (compare < 0) throw invalid("Pack requires a newer Studio engine");
        }
    }

    private void checkOntology(JsonNode document) {
        Set<String> objects = new HashSet<>();
        for (JsonNode object : document.path("objects")) {
            if (!objects.add(object.path("name").asText())) throw invalid("Duplicate ontology object");
            if (object.hasNonNull("id")) objects.add(object.path("id").asText());
        }
        for (JsonNode link : document.path("links")) {
            for (String field : List.of("from", "to")) {
                String ref = link.path(field).asText();
                if (!ref.isBlank() && !objects.contains(ref)) throw invalid("Unknown ontology link object: " + ref);
            }
        }
        for (String kind : List.of("metrics", "actions", "signals")) {
            for (JsonNode item : document.path(kind)) {
                String ref = item.path("object").asText();
                if (!ref.isBlank() && !objects.contains(ref)) throw invalid("Unknown ontology object: " + ref);
            }
        }
    }

    private void checkMetricReferences(JsonNode document) {
        JsonNode metrics = document.isArray() ? document : document.path("metrics");
        if (!metrics.isArray()) throw invalid("Metrics must contain indicator references");
        for (JsonNode metric : metrics) {
            if (metric.path("indicatorRef").isNull() || metric.path("indicatorRef").isMissingNode()
                    || metric.path("indicatorRef").asText().isBlank() || metric.has("sql"))
                throw invalid("Canonical metric definitions belong to Stack");
        }
    }

    public static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static PackException invalid(String message) { return new PackException(422, "PACK_INVALID", message); }
    private static PackException tooLarge() { return new PackException(413, "PACK_TOO_LARGE", "Pack exceeds size or entry limits"); }
}
