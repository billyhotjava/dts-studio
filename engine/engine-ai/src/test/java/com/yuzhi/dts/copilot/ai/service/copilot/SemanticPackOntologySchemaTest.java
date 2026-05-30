package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SemanticPackOntologySchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldTreatMissingOntologySectionsAsEmptyListsForLegacyPacks() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        SemanticPackService.SemanticPack procurement = service.getPack("procurement").orElseThrow();
        assertThat(procurement.links()).isEmpty();
        assertThat(procurement.metrics()).isEmpty();
        assertThat(procurement.signals()).isEmpty();
        assertThat(procurement.actions()).isEmpty();
    }

    @Test
    void shouldSkipInvalidOntologyEntriesWithoutDroppingValidEntries() throws Exception {
        SemanticPackService.SemanticPack pack = SemanticPackService.parsePack(objectMapper.readTree("""
                {
                  "domain": "demo",
                  "description": "demo pack",
                  "objects": [
                    {"name": "Customer", "view": "public.customer"}
                  ],
                  "links": [
                    {"name": "customer_to_project", "from": "Customer", "to": "Project", "fromKey": "customer_id", "toKey": "id", "cardinality": "one-to-many"},
                    {"name": "broken_link", "from": "Customer", "to": "Project"}
                  ],
                  "metrics": [
                    {"name": "baddebt_amount", "object": "Customer", "expr": "baddebt_amount", "caliber": "sum"},
                    {"name": "broken_metric", "object": "Customer"}
                  ],
                  "signals": [
                    {"name": "baddebt_risk", "object": "Customer", "severity": "high", "when": "baddebt_amount > 0", "advice": "create draft"},
                    {"name": "broken_signal", "object": "Customer", "severity": "high"}
                  ],
                  "actions": [
                    {
                      "name": "create_baddebt_draft",
                      "object": "Customer",
                      "intent": "create draft",
                      "endpoint": {"service": "adminapi", "draft": "/draft", "commit": "/commit"},
                      "params": [{"name": "customerId", "source": "customer_id", "required": true}],
                      "approval": "human",
                      "audit": true,
                      "guard": "flowerbiz:baddebt:draft"
                    },
                    {"name": "broken_action", "object": "Customer", "intent": "create draft"}
                  ]
                }
                """));

        assertThat(pack.links()).extracting(SemanticPackService.OntologyLink::name)
                .containsExactly("customer_to_project");
        assertThat(pack.metrics()).extracting(SemanticPackService.OntologyMetric::name)
                .containsExactly("baddebt_amount");
        assertThat(pack.signals()).extracting(SemanticPackService.OntologySignal::name)
                .containsExactly("baddebt_risk");
        assertThat(pack.actions()).extracting(SemanticPackService.OntologyAction::name)
                .containsExactly("create_baddebt_draft");
    }

    @Test
    void shouldLoadFlowerbizPackWithTier1Links() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        SemanticPackService.SemanticPack flowerbiz = service.getPack("flowerbiz").orElseThrow();
        assertThat(flowerbiz.links()).extracting(SemanticPackService.OntologyLink::name)
                .containsExactly("客户_项目", "项目_报花", "报花_采购", "报花_结算");
        assertThat(flowerbiz.links()).extracting(SemanticPackService.OntologyLink::cardinality)
                .containsExactly("1:N", "1:N", "1:N", "N:1");
        assertThat(flowerbiz.links()).extracting(SemanticPackService.OntologyLink::joinHint)
                .containsExactly("可能孤儿", "", "采购 flower_item_id 软外键", "biz_ids_json 多报花 JSON 数组需展开");
        assertThat(flowerbiz.objects()).extracting(SemanticPackService.SemanticObject::name)
                .contains("客户", "项目", "租赁报花明细", "采购明细", "结算单");
    }

    @Test
    void shouldLoadFlowerbizPackWithTier2Metrics() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        SemanticPackService.SemanticPack flowerbiz = service.getPack("flowerbiz").orElseThrow();
        assertThat(flowerbiz.metrics()).extracting(SemanticPackService.OntologyMetric::name)
                .containsExactly(
                        "租金净额",
                        "处理成本",
                        "销售金额",
                        "额外费用",
                        "坏账租金损失",
                        "项目坏账率",
                        "客户在租金额");
        assertThat(flowerbiz.metrics()).extracting(SemanticPackService.OntologyMetric::caliber)
                .anyMatch(caliber -> caliber.contains("dbt_amount:rent"))
                .anyMatch(caliber -> caliber.contains("dbt_amount:cost"))
                .anyMatch(caliber -> caliber.contains("dbt_amount:sale"))
                .anyMatch(caliber -> caliber.contains("dbt_amount:extra_cost"));
        assertThat(flowerbiz.metrics()).extracting(SemanticPackService.OntologyMetric::format)
                .contains("currency", "percent");

        Map<String, SemanticPackService.SemanticObject> objectsByName = flowerbiz.objects().stream()
                .collect(Collectors.toMap(SemanticPackService.SemanticObject::name, object -> object));
        for (SemanticPackService.OntologyMetric metric : flowerbiz.metrics()) {
            SemanticPackService.SemanticObject object = objectsByName.get(metric.object());
            assertThat(object).as("metric object exists: %s", metric.name()).isNotNull();
            assertThat(referencedFields(metric.expr()))
                    .as("metric expr fields exist in object dimensions/measures: %s", metric.name())
                    .allSatisfy(field -> assertThat(availableFields(object)).contains(field));
        }
    }

    @Test
    void shouldLoadFlowerbizPackWithTier2Signals() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        SemanticPackService.SemanticPack flowerbiz = service.getPack("flowerbiz").orElseThrow();
        assertThat(flowerbiz.signals()).extracting(SemanticPackService.OntologySignal::name)
                .containsExactly("坏账风险", "欠费预警");
        assertThat(flowerbiz.signals()).extracting(SemanticPackService.OntologySignal::severity)
                .containsExactly("high", "medium");
        assertThat(flowerbiz.signals()).allSatisfy(signal -> {
            assertThat(signal.advice()).isNotBlank();
            assertThat(flowerbiz.objects()).extracting(SemanticPackService.SemanticObject::name)
                    .contains(signal.object());
            assertThat(flowerbiz.metrics()).extracting(SemanticPackService.OntologyMetric::name)
                    .anySatisfy(metricName -> assertThat(signal.when()).contains(metricName));
        });
        assertThat(flowerbiz.signals().getFirst().linkedActions()).containsExactly("创建坏账处理单");
    }

    @Test
    void shouldLoadFlowerbizPackWithTier3Actions() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        SemanticPackService.SemanticPack flowerbiz = service.getPack("flowerbiz").orElseThrow();
        SemanticPackService.OntologyAction baddebtAction = flowerbiz.actions().stream()
                .filter(action -> "创建坏账处理单".equals(action.name()))
                .findFirst()
                .orElseThrow();

        assertThat(baddebtAction.object()).isEqualTo("租赁报花明细");
        assertThat(baddebtAction.endpoint().service()).isEqualTo("adminapi");
        assertThat(baddebtAction.endpoint().draft())
                .isEqualTo("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt");
        assertThat(baddebtAction.endpoint().commit())
                .isEqualTo("/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt");
        assertThat(baddebtAction.approval()).isEqualTo("human");
        assertThat(baddebtAction.audit()).isTrue();
        assertThat(baddebtAction.guard()).isEqualTo("flowerbiz:baddebt:draft");
        assertThat(baddebtAction.params()).extracting(SemanticPackService.OntologyActionParam::name)
                .containsExactly("projectId", "draftItemJson", "badDebtType");
        assertThat(baddebtAction.params()).filteredOn(SemanticPackService.OntologyActionParam::required)
                .hasSize(3);
        assertActionParamSourcesResolvable(flowerbiz, baddebtAction);
        assertThat(flowerbiz.signals().getFirst().linkedActions()).contains(baddebtAction.name());
    }

    private static Set<String> referencedFields(String expr) {
        java.util.regex.Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(expr);
        Set<String> fields = new LinkedHashSet<>();
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }
        return fields;
    }

    private static Set<String> availableFields(SemanticPackService.SemanticObject object) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(object.keyDimensions());
        fields.addAll(object.keyMeasures());
        fields.addAll(object.commonFilters());
        if (!object.defaultTimeField().isBlank()) {
            fields.add(object.defaultTimeField());
        }
        return fields;
    }

    private static void assertActionParamSourcesResolvable(
            SemanticPackService.SemanticPack pack,
            SemanticPackService.OntologyAction action) {
        Map<String, SemanticPackService.SemanticObject> objectsByName = pack.objects().stream()
                .collect(Collectors.toMap(SemanticPackService.SemanticObject::name, object -> object));
        for (SemanticPackService.OntologyActionParam param : action.params()) {
            String source = param.source();
            String objectName = action.object();
            String fieldName = source;
            int separator = source.lastIndexOf('.');
            if (separator > 0 && separator < source.length() - 1) {
                objectName = source.substring(0, separator);
                fieldName = source.substring(separator + 1);
            }
            SemanticPackService.SemanticObject object = objectsByName.get(objectName);
            assertThat(object).as("action param object exists: %s", param.name()).isNotNull();
            assertThat(availableFields(object)).as("action param source field exists: %s", param.name())
                    .contains(fieldName);
        }
    }
}
