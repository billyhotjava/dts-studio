package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaliberStaticSqlRegressionTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String FIXTURE_RESOURCE = "sprint31/caliber-static-sql-regression.json";

    private CaliberRuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CaliberRuleRegistry(OBJECT_MAPPER);
        registry.init();
    }

    @Test
    void shouldCoverEverySqlStaticCaliberRuleWithRejectAndAllowExamples() throws Exception {
        List<SqlRegressionFixture> fixtures = loadFixtures();
        Set<String> staticRuleIds = new LinkedHashSet<>();
        for (CaliberRuleRegistry.CaliberRule rule : registry.rules()) {
            if ("SQL_STATIC".equalsIgnoreCase(rule.check().type())) {
                staticRuleIds.add(rule.id());
            }
        }

        assertThat(fixtures)
                .extracting(SqlRegressionFixture::ruleId)
                .containsExactlyInAnyOrderElementsOf(staticRuleIds);

        for (SqlRegressionFixture fixture : fixtures) {
            assertThat(fixture.rejectedSql())
                    .as(fixture.ruleId() + " rejected examples")
                    .isNotEmpty();
            for (String rejectedSql : fixture.rejectedSql()) {
                CaliberRuleRegistry.CaliberValidation validation = registry.validateSql("finance", rejectedSql);

                assertThat(validation.allowed()).as(rejectedSql).isFalse();
                assertThat(validation.violations())
                        .as(rejectedSql)
                        .extracting(CaliberRuleRegistry.CaliberViolation::ruleId)
                        .contains(fixture.ruleId());
            }

            assertThat(fixture.allowedSql())
                    .as(fixture.ruleId() + " allowed examples")
                    .isNotEmpty();
            for (String allowedSql : fixture.allowedSql()) {
                CaliberRuleRegistry.CaliberValidation validation = registry.validateSql("finance", allowedSql);

                assertThat(validation.allowed()).as(allowedSql).isTrue();
                assertThat(validation.violations()).as(allowedSql).isEmpty();
            }
        }
    }

    private static List<SqlRegressionFixture> loadFixtures() throws Exception {
        try (InputStream is = CaliberStaticSqlRegressionTest.class
                .getClassLoader()
                .getResourceAsStream(FIXTURE_RESOURCE)) {
            assertThat(is).as(FIXTURE_RESOURCE).isNotNull();
            FixtureDocument document = OBJECT_MAPPER.readValue(is, FixtureDocument.class);
            return document.fixtures();
        }
    }

    private record FixtureDocument(List<SqlRegressionFixture> fixtures) {
        private FixtureDocument {
            fixtures = copyOrEmpty(fixtures);
        }
    }

    private record SqlRegressionFixture(String ruleId, List<String> rejectedSql, List<String> allowedSql) {
        private SqlRegressionFixture {
            rejectedSql = copyOrEmpty(rejectedSql);
            allowedSql = copyOrEmpty(allowedSql);
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
