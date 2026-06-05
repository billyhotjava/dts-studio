package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticPackGovernanceGuardrailTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposeFinanceGuardrailsFromGovernanceGeneratedSection() throws Exception {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(objectMapper);
        registry.init();

        JsonNode financePack = readPack("semantic-packs/finance.json");
        assertGeneratedSectionMatchesRegistry(financePack, registry.guardrailsForDomain("finance"));

        SemanticPackService service = new SemanticPackService(objectMapper);
        service.init();

        SemanticPackService.SemanticPack finance = service.getPack("finance").orElseThrow();
        assertStartsWith(finance.guardrails(), registry.guardrailsForDomain("finance"));
        assertThat(finance.guardrails())
                .contains("默认只用 PostgreSQL 语法。时间月份用 to_char(date, 'YYYY-MM')，模糊匹配用 ILIKE 或 LIKE，不要使用 MySQL 日期函数。")
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldExposeProcurementGuardrailsFromGovernanceGeneratedSection() throws Exception {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(objectMapper);
        registry.init();

        JsonNode procurementPack = readPack("semantic-packs/procurement.json");
        assertGeneratedSectionMatchesRegistry(procurementPack, registry.guardrailsForDomain("procurement"));

        SemanticPackService service = new SemanticPackService(objectMapper);
        service.init();

        SemanticPackService.SemanticPack procurement = service.getPack("procurement").orElseThrow();
        assertStartsWith(procurement.guardrails(), registry.guardrailsForDomain("procurement"));
        assertThat(procurement.guardrails())
                .contains("不要使用 t_purchase_info.title like '%产品名%' 作为采购产品过滤条件。")
                .doesNotHaveDuplicates();
    }

    private JsonNode readPack(String resource) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(is).as(resource).isNotNull();
            return objectMapper.readTree(is);
        }
    }

    private static void assertGeneratedSectionMatchesRegistry(JsonNode pack, List<String> expectedGuardrails) {
        JsonNode generated = pack.path("generatedGuardrails");
        assertThat(generated.path("_generated").asBoolean()).isTrue();
        assertThat(generated.path("_source").asText()).isEqualTo("governance/caliber-rules.v1.json");
        assertThat(readTextArray(generated.path("rules"))).containsExactlyElementsOf(expectedGuardrails);
    }

    private static void assertStartsWith(List<String> actual, List<String> prefix) {
        assertThat(actual).hasSizeGreaterThanOrEqualTo(prefix.size());
        assertThat(actual.subList(0, prefix.size())).containsExactlyElementsOf(prefix);
    }

    private static List<String> readTextArray(JsonNode array) {
        assertThat(array.isArray()).isTrue();
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }
}
