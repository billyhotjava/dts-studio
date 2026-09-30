package com.yuzhi.dts.copilot.ai.service.pack;

import com.yuzhi.dts.copilot.ai.service.auth.ApiKeyService;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticPackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/** Shared real-runtime assertions; each lane must use its own disposable PostgreSQL. */
abstract class PackRuntimeTestSupport {
    protected boolean freshBootstrap() { return false; }
    @Autowired org.springframework.core.env.Environment environment;
    @Autowired TestRestTemplate http;
    @Autowired ApiKeyService keys;
    @Autowired SemanticPackService semantics;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.yuzhi.dts.copilot.ai.repository.Nl2SqlQueryTemplateRepository templates;
    @Autowired com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService matcher;

    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) throws Exception {
        assertThat(System.getenv("PG_DB")).isEqualTo("studio_test");
        assertThat(System.getenv("PG_HOST")).isEqualTo("127.0.0.1");
        assertThat(System.getProperty("studio.pack.archive")).isNotBlank();
        try(var connection=DriverManager.getConnection("jdbc:postgresql://127.0.0.1:%s/studio_test".formatted(System.getenv("PG_PORT")),System.getenv("PG_USER"),System.getenv("PG_PASSWORD"))) {
            connection.createStatement().execute("CREATE SCHEMA copilot_ai");
        }
    }

    @Test void authenticatedInstallAndActivateUseRealJpaTransactionsAndResolver() throws Exception {
        assertThat(semantics.getDomains()).isEmpty();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var legacySemantics = new SemanticPackService(mapper); legacySemantics.init();
        Map<String,String> before = new java.util.LinkedHashMap<>();
        if (freshBootstrap()) {
            assertThat(environment.getProperty("dts.studio.pack.fallback-classpath", Boolean.class)).isFalse();
            assertThat(environment.getActiveProfiles()).contains("studio-pack");
            for (String table : java.util.List.of("nl2sql_query_template", "nl2sql_routing_rule", "biz_enum_dictionary")) {
                assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai." + table, Integer.class))
                        .as("Unseeded " + table).isZero();
            }
            assertThat(System.getProperty("studio.pack.golden-input")).isNotBlank();
            before.putAll(mapper.readValue(java.nio.file.Files.readString(
                    Path.of(System.getProperty("studio.pack.golden-input"))),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>() {}));
        }
        for (var template : templates.findByIsActiveTrueOrderByPriorityDesc()) {
            for (var sample : mapper.readTree(template.getQuestionSamples())) {
                var match = matcher.match(sample.asText());
                before.put(sample.asText(), match.matched() ? match.template().getTemplateCode()+"|"+match.resolvedSql() : "unmatched");
            }
        }
        assertThat(before.size()).isEqualTo(106);
        if (!freshBootstrap() && System.getProperty("studio.pack.golden-output") != null) {
            java.nio.file.Files.writeString(Path.of(System.getProperty("studio.pack.golden-output")),
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(before),
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        }
        assertThat(http.getForEntity("/api/ai/packs",String.class).getStatusCode().value()).isEqualTo(401);
        var key=keys.generateKey("pack-smoke","isolated test","test",1);
        HttpHeaders headers=new HttpHeaders();
        headers.setBearerAuth(key.rawKey()); headers.set("X-Admin-Secret","isolated-pack-smoke-secret");
        var body=new LinkedMultiValueMap<String,Object>();
        body.add("file",new FileSystemResource(Path.of(System.getProperty("studio.pack.archive"))));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var installed=http.postForEntity("/api/ai/packs",new HttpEntity<>(body,headers),Map.class);
        assertThat(installed.getStatusCode().value()).as(String.valueOf(installed.getBody())).isEqualTo(201);
        assertThat(http.postForEntity("/api/ai/packs",new HttpEntity<>(body,headers),Map.class).getStatusCode().value()).isEqualTo(200);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var activated=http.postForEntity("/api/ai/packs/prs-flower/versions/"+installed.getBody().get("version")+"/activate",new HttpEntity<>(Map.of(),headers),Map.class);
        assertThat(activated.getStatusCode().value()).as(String.valueOf(activated.getBody())).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai.nl2sql_query_template WHERE source='pack'",Integer.class)).isEqualTo(57);
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
        while(semantics.getDomains().isEmpty() && System.nanoTime()<deadline) Thread.sleep(100);
        assertThat(semantics.getDomains()).hasSize(5).isEqualTo(legacySemantics.getDomains());
        for (String domain : semantics.getDomains()) {
            var expected = mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(legacySemantics.getPack(domain).orElseThrow());
            for (var action : expected.path("actions")) {
                ((com.fasterxml.jackson.databind.node.ObjectNode)action.path("endpoint")).put("service", "prs-legacy-adminapi");
            }
            assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(semantics.getPack(domain).orElseThrow())).isEqualTo(expected);
        }
        var differences = new java.util.ArrayList<String>();
        before.forEach((question,expected) -> {
            var match=matcher.match(question);
            String actual = match.matched() ? match.template().getTemplateCode()+"|"+match.resolvedSql() : "unmatched";
            if (!actual.equals(expected)) differences.add(question + ": expected " + expected.split("\\|", 2)[0]
                    + ", actual " + actual.split("\\|", 2)[0] + " (code or SQL differs)");
        });
        assertThat(differences).as("All template codes and SQL must match the captured legacy baseline").isEmpty();
        try (var scope = PackReadScope.open()) {
            matcher.match(before.keySet().iterator().next());
            assertThat(scope.sources()).containsExactly(new PackSourceRef(
                    "prs-flower", installed.getBody().get("version").toString()));
        }
        System.out.println("PACK_GOLDEN_QUESTIONS="+before.size());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai.studio_audit_outbox",Integer.class)).isEqualTo(2);

        var expectedRefs = mapper.valueToTree(java.util.List.of(new PackSourceRef(
                "prs-flower", installed.getBody().get("version").toString())));
        var request = Map.of("sessionId", "pack-provenance-http", "userId", "pack-smoke",
                "message", "你能分析哪些业务");
        var chat = http.postForEntity("/api/ai/agent/chat/send", new HttpEntity<>(request, headers), String.class);
        assertThat(chat.getStatusCode().value()).as(chat.getBody()).isEqualTo(200);
        assertThat(mapper.readTree(chat.getBody()).path("packRefs")).isEqualTo(expectedRefs);
        var history = http.exchange("/api/ai/agent/chat/pack-provenance-http", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(history.getStatusCode().value()).isEqualTo(200);
        var assistant = mapper.readTree(history.getBody()).path("messages").get(1);
        assertThat(assistant.path("packRefs")).isEqualTo(expectedRefs);
        assertThat(assistant.path("trace").path("packRefs")).isEqualTo(expectedRefs);

        var streamRequest = Map.of("sessionId", "pack-provenance-sse", "userId", "pack-smoke",
                "message", "你能分析哪些业务");
        var stream = http.postForEntity("/api/ai/agent/chat/stream",
                new HttpEntity<>(streamRequest, headers), String.class);
        assertThat(stream.getStatusCode().value()).as(stream.getBody()).isEqualTo(200);
        assertThat(stream.getBody()).contains("event: done\ndata: ");
        String done = stream.getBody().split("event: done\ndata: ", 2)[1].strip();
        assertThat(mapper.readTree(done).path("packRefs")).isEqualTo(expectedRefs);
        assertThat(mapper.readTree(done).path("trace").path("packRefs")).isEqualTo(expectedRefs);
        assertBootstrapModeSafety();
    }
    private void assertBootstrapModeSafety() throws Exception {
        var dataSource = java.util.Objects.requireNonNull(jdbc.getDataSource());
        int history = jdbc.queryForObject("SELECT count(*) FROM copilot_ai.databasechangelog", Integer.class);
        var templatesBefore = jdbc.queryForList("SELECT template_code,sql_template,source_pack_version_id "
                + "FROM copilot_ai.nl2sql_query_template ORDER BY template_code");
        if (freshBootstrap()) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai.studio_pack_bootstrap", Integer.class))
                    .isEqualTo(1);
            try (var connection = dataSource.getConnection()) {
                connection.setSchema("copilot_ai");
                try (var migration = new liquibase.Liquibase("config/liquibase/master.xml",
                        new liquibase.resource.ClassLoaderResourceAccessor(),
                        new liquibase.database.jvm.JdbcConnection(connection))) {
                    migration.setChangeLogParameter("studioPackMode", "true");
                    migration.update("studio-pack");
                }
            }
        }
        try (var connection = dataSource.getConnection()) {
            connection.setSchema("copilot_ai");
            try (var migration = new liquibase.Liquibase("config/liquibase/master.xml",
                    new liquibase.resource.ClassLoaderResourceAccessor(),
                    new liquibase.database.jvm.JdbcConnection(connection))) {
                if (!freshBootstrap()) migration.setChangeLogParameter("studioPackMode", "true");
                assertThatThrownBy(() -> migration.update(freshBootstrap() ? "" : "studio-pack"))
                        .hasStackTraceContaining(freshBootstrap() ? "requires the studio-pack profile"
                                : "without legacy seed history");
            }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai.databasechangelog", Integer.class))
                .isEqualTo(history);
        assertThat(jdbc.queryForList("SELECT template_code,sql_template,source_pack_version_id "
                + "FROM copilot_ai.nl2sql_query_template ORDER BY template_code")).isEqualTo(templatesBefore);
    }

}
