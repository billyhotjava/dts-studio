package com.yuzhi.dts.copilot.ai.service.pack;

import liquibase.ChecksumVersion;
import liquibase.Contexts;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.filter.ContextChangeSetFilter;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PackMigrationCompatibilityTest {
    @Test
    void allPreProfileChangesetsKeepTheirPublishedChecksums() throws Exception {
        Map<String,String> expected = new LinkedHashMap<>();
        try (var input = getClass().getResourceAsStream("/config/liquibase/pre-pack-profile-checksums.tsv")) {
            assertThat(input).isNotNull();
            for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] entry = line.split("\t", 2);
                expected.put(entry[0], entry[1]);
            }
        }
        assertThat(expected).hasSize(61);
        var actual = new LinkedHashMap<String,String>();
        for (var change : changelog().getChangeSets()) {
            String key = change.getFilePath()+"::"+change.getId()+"::"+change.getAuthor();
            if (expected.containsKey(key)) actual.put(key, change.generateCheckSum(ChecksumVersion.latest()).toString());
        }
        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @Test
    void packContextSkipsOnlyLegacySeedsAndKeepsSchemaWhileDefaultLaneRemainsCompatible() throws Exception {
        var fresh = new ContextChangeSetFilter(new Contexts("studio-pack"));
        var legacy = new ContextChangeSetFilter(new Contexts());
        int seeds=0, freshChanges=0, legacyChanges=0;
        for (var change : changelog().getChangeSets()) {
            if (fresh.accepts(change).isAccepted()) freshChanges++;
            if (legacy.accepts(change).isAccepted()) legacyChanges++;
            if (change.getContexts().toString().equals("legacy-domain-seed")) {
                seeds++;
                assertThat(change.getChanges()).allMatch(item -> item instanceof liquibase.change.core.RawSQLChange);
                assertThat(fresh.accepts(change).isAccepted()).as(change.getId()).isFalse();
                assertThat(legacy.accepts(change).isAccepted()).as(change.getId()).isTrue();
            }
        }
        assertThat(seeds).isEqualTo(24);
        assertThat(freshChanges).isEqualTo(39);
        assertThat(legacyChanges).isEqualTo(62);
    }

    private DatabaseChangeLog changelog() throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            String file = "config/liquibase/master.xml";
            return ChangeLogParserFactory.getInstance().getParser(file, resources)
                    .parse(file, new ChangeLogParameters(), resources);
        }
    }
}
