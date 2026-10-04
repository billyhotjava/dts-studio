package com.yuzhi.dts.copilot.ai.service.pack;

import com.yuzhi.dts.common.pack.PackException;

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
