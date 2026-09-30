package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlQueryTemplateRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.CaliberRuleRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticPackService;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackProvenanceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void cachedOntologyRetainsItsReadVersionAndReportsBothVersionsIfBothWereRead() throws Exception {
        var initial = ontology("1.0.0");
        var active = new AtomicReference<>(new PackAssetResolver.Snapshot(1, List.of(initial)));
        var semantics = new SemanticPackService(mapper);
        semantics.setPackResourceReader(new PackResourceReader(active::get, false));
        semantics.getPack("fixture"); // Warm the parsed cache outside an execution.
        List<PackSourceRef> original;
        try (var scope = PackReadScope.open()) {
            semantics.getPack("fixture");
            active.set(new PackAssetResolver.Snapshot(2, List.of(ontology("2.0.0"))));
            original = scope.sources();
            assertThat(original).containsExactly(new PackSourceRef("fixture", "1.0.0"));
            semantics.getPack("fixture");
            assertThat(scope.sources()).containsExactly(
                    new PackSourceRef("fixture", "1.0.0"), new PackSourceRef("fixture", "2.0.0"));
        }
        assertThat(original).containsExactly(new PackSourceRef("fixture", "1.0.0"));
        try (var scope = PackReadScope.open()) {
            semantics.getPack("fixture");
            assertThat(scope.sources()).containsExactly(new PackSourceRef("fixture", "2.0.0"));
        }
        assertThat(PackReadScope.currentSources()).isEmpty();
    }

    @Test
    void cachedGovernanceRecordsOnlyPacksBackingItsLoadedResources() throws Exception {
        String key = "governance/caliber-rules.v1.json";
        String json = Files.readString(Path.of("src/main/resources", key));
        var rules = new CaliberRuleRegistry(mapper);
        var used = new PackAssetResolver.Asset("governance", "1.0.0", "guardrails", key, json, mapper.readTree(json));
        var unused = ontology("9.0.0");
        rules.setPackResourceReader(new PackResourceReader(
                () -> new PackAssetResolver.Snapshot(1, List.of(used, unused)), false));
        rules.rules();
        try (var scope = PackReadScope.open()) {
            assertThat(rules.rules()).isNotEmpty();
            rules.rules();
            assertThat(scope.sources()).containsExactly(new PackSourceRef("governance", "1.0.0"));
        }
    }

    @Test
    void promptReadRecordsSourceButLegacyFallbackDoesNotInventPackVersion() throws Exception {
        var asset = new PackAssetResolver.Asset("prompt-pack", "3.0.0", "prompts", "prompts/test.md", "content", null);
        var reader = new PackResourceReader(() -> new PackAssetResolver.Snapshot(4, List.of(asset)), false);
        try (var scope = PackReadScope.open(); var stream = reader.open(asset.key(), reader.snapshot())) {
            assertThat(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("content");
            assertThat(scope.sources()).containsExactly(new PackSourceRef("prompt-pack", "3.0.0"));
        }
        var fallback = new SemanticPackService(mapper);
        fallback.init();
        try (var scope = PackReadScope.open()) {
            assertThat(fallback.getDomains()).isNotEmpty();
            assertThat(scope.sources()).isEmpty();
        }
    }

    @Test
    void nestedScopesAndReusedWorkerThreadsDoNotLeakSourcesAfterFailure() throws Exception {
        var ref = new PackSourceRef("fixture", "1.0.0");
        try (var outer = PackReadScope.open()) {
            PackReadScope.record(List.of(ref));
            try (var inner = PackReadScope.open()) {
                assertThat(inner.sources()).isEmpty();
                PackReadScope.record(List.of(new PackSourceRef("nested", "2.0.0")));
            }
            assertThat(outer.sources()).containsExactly(ref);
            try (var worker = Executors.newSingleThreadExecutor()) {
                worker.submit(() -> {
                    assertThat(PackReadScope.currentSources()).isEmpty();
                    assertThatThrownBy(() -> {
                        try (var scope = PackReadScope.open()) {
                            PackReadScope.record(List.of(ref));
                            throw new IllegalStateException("fixture failure");
                        }
                    }).hasMessage("fixture failure");
                }).get(5, TimeUnit.SECONDS);
                assertThat(worker.submit(PackReadScope::currentSources).get(5, TimeUnit.SECONDS)).isEmpty();
            }
        }
        assertThat(PackReadScope.currentSources()).isEmpty();
    }

    @Test
    void templateProvenanceUsesPersistedOwnershipEvenIfActivePackAlreadyChanged() {
        var repository = mock(Nl2SqlQueryTemplateRepository.class);
        var template = new Nl2SqlQueryTemplate();
        template.setTemplateCode("FIXTURE"); template.setDomain("fixture");
        template.setIntentPatterns("[\"fixture question\"]"); template.setQuestionSamples("[]");
        template.setParameters("[]"); template.setSqlTemplate("select 1"); template.setSourcePackVersionId(7L);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of(template));
        var owner = mock(Nl2SqlQueryTemplateRepository.TemplatePackSource.class);
        when(owner.getVersionId()).thenReturn(7L);
        when(owner.getPackName()).thenReturn("fixture"); when(owner.getPackVersion()).thenReturn("1.0.0");
        when(repository.findPackSources(Set.of(7L))).thenReturn(List.of(owner));
        var matcher = new TemplateMatcherService(repository, mapper);
        matcher.setPackAssets(() -> new PackAssetResolver.Snapshot(2, List.of()));
        assertThat(matcher.match("fixture question").matched()).isTrue();
        try (var scope = PackReadScope.open()) {
            assertThat(matcher.match("fixture question").resolvedSql()).isEqualTo("select 1");
            assertThat(scope.sources()).containsExactly(new PackSourceRef("fixture", "1.0.0"));
        }
        verify(repository, times(1)).findPackSources(Set.of(7L));
        template.setSourcePackVersionId(null); // Manual rows never inherit the active Pack's provenance.
        matcher.setPackAssets(() -> new PackAssetResolver.Snapshot(3, List.of()));
        try (var scope = PackReadScope.open()) {
            matcher.match("fixture question");
            assertThat(scope.sources()).isEmpty();
        }
    }

    @Test
    void missingTemplateOwnershipFailsInsteadOfGuessingCurrentPack() {
        var repository = mock(Nl2SqlQueryTemplateRepository.class);
        var template = new Nl2SqlQueryTemplate(); template.setSourcePackVersionId(7L);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of(template));
        var matcher = new TemplateMatcherService(repository, mapper);
        assertThatThrownBy(() -> matcher.match("fixture question"))
                .hasMessage("Template Pack ownership is unavailable");
    }

    private PackAssetResolver.Asset ontology(String version) throws Exception {
        String json = "{\"domain\":\"fixture\",\"objects\":[]}";
        return new PackAssetResolver.Asset("fixture", version, "ontology", "semantic-packs/fixture.json",
                json, mapper.readTree(json));
    }
}
