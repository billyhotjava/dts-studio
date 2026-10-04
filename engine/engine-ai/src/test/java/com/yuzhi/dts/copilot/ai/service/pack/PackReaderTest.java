package com.yuzhi.dts.copilot.ai.service.pack;

import com.yuzhi.dts.common.pack.PackException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticPackService;
import com.yuzhi.dts.copilot.ai.service.copilot.CaliberRuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class PackReaderTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<PackAssetResolver.Snapshot> snapshot = new AtomicReference<>(new PackAssetResolver.Snapshot(0,List.of()));
    private final PackResourceReader reader = new PackResourceReader(snapshot::get, false);

    @Test void semanticActivationRollbackAndRemovalRefreshWholeGeneration() throws Exception {
        var semantic = new SemanticPackService(mapper);
        semantic.setPackResourceReader(reader); semantic.init();
        assertThat(semantic.getDomains()).isEmpty();
        snapshot.set(new PackAssetResolver.Snapshot(1,List.of(asset("a", "one"))));
        assertThat(semantic.getDomains()).containsExactly("a");
        assertThat(semantic.getContextForDomain("a")).contains("one");
        snapshot.set(new PackAssetResolver.Snapshot(2,List.of(asset("b", "two"))));
        assertThat(semantic.getDomains()).containsExactly("b");
        assertThat(semantic.getPack("a")).isEmpty();
        snapshot.set(new PackAssetResolver.Snapshot(3,List.of(asset("a", "one"))));
        assertThat(semantic.getContextForDomain("a")).contains("one");
        semantic.setPackResourceReader(new PackResourceReader(() -> { throw new IllegalStateException("registry unavailable"); },true));
        assertThatThrownBy(semantic::getDomains).hasMessage("registry unavailable");
    }

    @Test void fallbackStopsAfterFirstActivationAndMissingRulesFailClosed() {
        var rules = new CaliberRuleRegistry(mapper);
        rules.setPackResourceReader(new PackResourceReader(snapshot::get,true)); rules.init();
        assertThat(rules.rules()).isNotEmpty();
        snapshot.set(new PackAssetResolver.Snapshot(1,List.of()));
        assertThatThrownBy(() -> rules.validateSql("finance","select 1"))
                .hasRootCauseInstanceOf(PackException.class);
    }

    @ParameterizedTest
    @ValueSource(strings={"VoucherLedgerTieoutRegistry", "FinanceVoucherSubjectTieoutRegistry",
            "FinanceAnswerAuditTrailRegistry", "FinanceWeakPathReconciliationCandidateRegistry",
            "BusinessDirectResponseCatalogService", "FinanceInvariantRegistry", "FinanceSummaryDualReconciliationRegistry",
            "Nl2SqlAccuracyGoldenSetRegistry", "FinanceSignoffBaselineRegistry", "FinanceAmountColumnAlignmentRegistry",
            "FinanceInvariantRegressionService", "CaliberRuleRegistry", "FinanceAuthorityRegistry",
            "FinanceReconciliationScorecardRegistry", "FinanceDetailReconciliationSampleRegistry",
            "FinanceDifferentialGridRegistry", "FinanceApplicationMysqlAuthorityRegistry"})
    void packAndLegacyReadersProduceSameTypedAssets(String name) throws Exception {
        List<PackAssetResolver.Asset> assets = new ArrayList<>();
        Path resources = Path.of("src/main/resources");
        for (String folder : List.of("governance","planner")) {
            try (var files=Files.list(resources.resolve(folder))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                    String key=folder+"/"+file.getFileName(); String text=Files.readString(file);
                    assets.add(new PackAssetResolver.Asset("prs","1.0.0",PackResourceReader.kind(key),key,text,mapper.readTree(text)));
                }
            }
        }
        snapshot.set(new PackAssetResolver.Snapshot(1,assets));
        Class<?> type=Class.forName("com.yuzhi.dts.copilot.ai.service.copilot."+name);
        Object legacy=construct(type,new HashMap<>(),false);
        Object active=construct(type,new HashMap<>(),true);
        int checked=0;
        for (var method:type.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers()) && method.getParameterCount()==0
                    && (List.class.isAssignableFrom(method.getReturnType()))) {
                assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(method.invoke(active))).as(name+"."+method.getName())
                        .isEqualTo(mapper.valueToTree(method.invoke(legacy)));
                checked++;
            }
        }
        assertThat(checked).isPositive();
    }

    private Object construct(Class<?> type, Map<Class<?>,Object> instances, boolean managed) throws Exception {
        if (type == ObjectMapper.class) return mapper;
        if (instances.containsKey(type)) return instances.get(type);
        var constructor=type.getConstructors()[0];
        Object[] arguments=new Object[constructor.getParameterCount()];
        for(int i=0;i<arguments.length;i++) arguments[i]=construct(constructor.getParameterTypes()[i],instances,managed);
        Object instance=constructor.newInstance(arguments); instances.put(type,instance);
        if(managed) ((PackBackedJsonRegistry)instance).setPackResourceReader(reader);
        type.getMethod("init").invoke(instance);
        return instance;
    }

    private PackAssetResolver.Asset asset(String domain,String description) throws Exception {
        String json="{\"domain\":\""+domain+"\",\"description\":\""+description+"\",\"objects\":[]}";
        return new PackAssetResolver.Asset("fixture",description,"ontology",domain,json,mapper.readTree(json));
    }
}
