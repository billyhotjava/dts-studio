package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.assertj.core.api.Assertions.*;

class PackArchiveValidatorTest {
    private final PackArchiveValidator validator = new PackArchiveValidator();

    static byte[] pack(String name, String version, String ontology) throws Exception {
        String manifest = """
                {"apiVersion":"dts.pack/v1","name":"%s","version":"%s","vendor":"fixture",
                 "requires":{"dts-studio":">=1.0.0"},
                 "capabilities":{"ontology":[{"path":"ontology/domain.json","key":"fixture","schemaVersion":1}]}}
                """.formatted(name, version);
        return archive(new LinkedHashMap<>(Map.of("pack-manifest.yaml", manifest.getBytes(StandardCharsets.UTF_8),
                "ontology/domain.json", ontology.getBytes(StandardCharsets.UTF_8))), true);
    }

    static byte[] archive(Map<String, byte[]> files, boolean hashes) throws Exception {
        files = new LinkedHashMap<>(files);
        if (hashes) {
            StringBuilder sums = new StringBuilder();
            files.forEach((key, value) -> sums.append(PackArchiveValidator.sha256(value)).append("  ").append(key).append('\n'));
            files.put("SHA256SUMS", sums.toString().getBytes(StandardCharsets.UTF_8));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (var entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey())); zip.write(entry.getValue()); zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test void acceptsValidPackAndPreservesProvenance() throws Exception {
        var result = validator.validate(new ByteArrayInputStream(pack("fixture", "1.0.0", "{\"domain\":\"fixture\",\"objects\":[]}")));
        assertThat(result.name()).isEqualTo("fixture");
        assertThat(result.assets()).hasSize(1);
        assertThat(result.assets().getFirst().json().path("domain").asText()).isEqualTo("fixture");
    }

    @ParameterizedTest @ValueSource(strings={"../escape", "/absolute", "a/../b", "a\\b", "C:/file", "a//b", "a/./b"})
    void rejectsUnsafePaths(String path) throws Exception {
        byte[] zip = archive(Map.of(path, new byte[]{1}), false);
        assertThatThrownBy(() -> validator.validate(new ByteArrayInputStream(zip))).isInstanceOf(PackException.class);
    }

    @Test void rejectsCompressedBombBeforeParsing() throws Exception {
        byte[] zip = archive(Map.of("bomb", new byte[PackArchiveValidator.MAX_ENTRY + 1]), false);
        assertThatThrownBy(() -> validator.validate(new ByteArrayInputStream(zip)))
                .isInstanceOfSatisfying(PackException.class, e -> assertThat(e.status()).isEqualTo(413));
    }

    @Test void rejectsMissingManifestNameAndUnknownLink() throws Exception {
        assertThatThrownBy(() -> validator.validate(new ByteArrayInputStream(pack("", "1.0.0", "{\"domain\":\"d\",\"objects\":[]}"))))
                .isInstanceOf(PackException.class);
        assertThatThrownBy(() -> validator.validate(new ByteArrayInputStream(pack("fixture", "1.0.0",
                "{\"domain\":\"d\",\"objects\":[{\"name\":\"a\"}],\"links\":[{\"from\":\"missing\",\"to\":\"a\"}]}"))))
                .isInstanceOf(PackException.class);
    }

    @Test void detectsTampering() throws Exception {
        var valid = pack("fixture", "1.0.0", "{\"domain\":\"d\",\"objects\":[]}");
        Map<String,byte[]> files = new LinkedHashMap<>();
        try (var input = new java.util.zip.ZipInputStream(new ByteArrayInputStream(valid))) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) files.put(entry.getName(), input.readAllBytes());
        }
        files.put("ontology/domain.json", "{\"domain\":\"tampered\",\"objects\":[]}".getBytes(StandardCharsets.UTF_8));
        byte[] zip = archive(files, false);
        assertThatThrownBy(() -> validator.validate(new ByteArrayInputStream(zip)))
                .isInstanceOfSatisfying(PackException.class, error -> assertThat(error.errors()).anyMatch(message -> message.contains("Checksum mismatch")));

    }

    @Test void actionContractRequiresBoundServiceReferenceAndHumanApprovalForHighRisk() throws Exception {
        var action = new ObjectMapper().readTree("""
                {"name":"draft","object":"fixture","riskLevel":"high","intent":"test","params":[],
                 "target":{"serviceRef":"prs-api","draft":{"method":"POST","path":"/draft"},"commit":{"method":"POST","path":"/commit"}},
                 "approval":{"mode":"HITL","requiredRole":"PRS_FINANCE"}}
                """);
        validator.validateSchema("assets/action.v1.schema.json",action);
        ((com.fasterxml.jackson.databind.node.ObjectNode)action.path("approval")).put("mode","AUTO");
        assertThatThrownBy(() -> validator.validateSchema("assets/action.v1.schema.json",action)).isInstanceOf(PackException.class);
        ((com.fasterxml.jackson.databind.node.ObjectNode)action.path("approval")).put("mode","HITL");
        ((com.fasterxml.jackson.databind.node.ObjectNode)action.path("target").path("draft")).put("path","//external-host/draft");
        assertThatThrownBy(() -> validator.validateSchema("assets/action.v1.schema.json",action)).isInstanceOf(PackException.class);
    }

    @ParameterizedTest @ValueSource(strings={"project-fulfillment", "field-operations", "procurement", "warehouse", "finance", "flowerbiz"})
    void existingSemanticPackSatisfiesSchema(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/semantic-packs/" + name + ".json")) {
            var document = new ObjectMapper().readTree(stream);
            validator.validateSchema("assets/ontology.v1.schema.json", document);
            var valid = validator.validate(new ByteArrayInputStream(pack("fixture", "1.0.0", document.toString())));
            assertThat(valid.assets()).hasSize(1);
        }
    }
}
