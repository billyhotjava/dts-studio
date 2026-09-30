package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;

class PackRegistryIntegrationTest {
    private JdbcTemplate jdbc;
    private PackRegistryService registry;
    private RegistryPackAssetResolver resolver;
    private DriverManagerDataSource dataSource;

    @BeforeEach void setup() throws Exception {
        assertThat(System.getenv("PG_DB")).as("Only the disposable Studio test database is allowed").isEqualTo("studio_test");
        assertThat(System.getenv("PG_HOST")).isEqualTo("127.0.0.1");
        dataSource = new DriverManagerDataSource("jdbc:postgresql://%s:%s/%s".formatted(
                System.getenv("PG_HOST"), System.getenv("PG_PORT"), System.getenv("PG_DB")),
                System.getenv("PG_USER"), System.getenv("PG_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP SCHEMA IF EXISTS copilot_ai CASCADE");
        jdbc.execute("CREATE SCHEMA copilot_ai");
        try (var connection = dataSource.getConnection()) {
            connection.setSchema("copilot_ai");
            try (var liquibase = new Liquibase("config/liquibase/pack-registry-test.xml",
                    new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) { liquibase.update(""); }
        }
        var manager = new DataSourceTransactionManager(dataSource);
        registry = new PackRegistryService(jdbc, manager, new ObjectMapper());
        resolver = new RegistryPackAssetResolver(jdbc, new ObjectMapper(), manager);
    }

    private PackArchiveValidator.ValidatedPack pack(String version) throws Exception {
        return new PackArchiveValidator().validate(new ByteArrayInputStream(PackArchiveValidatorTest.pack("fixture", version,
                "{\"domain\":\"fixture\",\"objects\":[],\"description\":\"" + version + "\"}")));
    }

    @Test void installsIdempotentlyActivatesAndRollsBackWithAudit() throws Exception {
        var first = pack("1.0.0");
        assertThat(registry.install(first,"admin").created()).isTrue();
        assertThat(registry.install(first,"admin").created()).isFalse();
        registry.activate("fixture","1.0.0","admin");
        assertThat(resolver.resolve("ontology","fixture").orElseThrow().packVersion()).isEqualTo("1.0.0");
        registry.install(pack("1.0.1"),"admin");
        assertThat(registry.activate("fixture","1.0.1","admin").previousVersion()).isEqualTo("1.0.0");
        assertThat(registry.rollback("fixture","admin").version()).isEqualTo("1.0.0");
        assertThat(registry.versions(null,null)).hasSize(2);
        assertThat(registry.assets("fixture","1.0.0",null)).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai.studio_audit_outbox",Integer.class)).isEqualTo(5);
        assertThatThrownBy(() -> registry.activate("fixture","1.0.0","admin"))
                .isInstanceOfSatisfying(PackException.class,e -> assertThat(e.code()).isEqualTo("PACK_ALREADY_ACTIVE"));
    }

    @Test void concurrentActivationHasExactlyOneActiveVersion() throws Exception {
        registry.install(pack("1.0.0"),"admin"); registry.install(pack("1.0.1"),"admin");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a=executor.submit(() -> registry.activate("fixture","1.0.0","a"));
            var b=executor.submit(() -> registry.activate("fixture","1.0.1","b"));
            a.get();b.get();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai.studio_pack_version WHERE status='ACTIVE'",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT generation FROM copilot_ai.studio_pack_generation WHERE id=1",Long.class)).isEqualTo(2L);
    }

    @Test void auditFailureRollsBackActivationAndGeneration() throws Exception {
        registry.install(pack("1.0.0"),"admin");
        jdbc.execute("ALTER TABLE copilot_ai.studio_audit_outbox ADD CONSTRAINT reject_activation CHECK (event->>'type' <> 'dts.pack.activated')");
        assertThatThrownBy(() -> registry.activate("fixture","1.0.0","admin")).isInstanceOf(RuntimeException.class);
        assertThat(registry.versions("fixture","1.0.0").getFirst().status()).isEqualTo("INSTALLED");
        assertThat(jdbc.queryForObject("SELECT generation FROM copilot_ai.studio_pack_generation WHERE id=1",Long.class)).isZero();
    }

    @Test void everyReplicaRefreshesWithinTenSecondsAndRollbackRemovesNewAssets() throws Exception {
        var clock = new java.util.concurrent.atomic.AtomicLong();
        var manager = new DataSourceTransactionManager(dataSource);
        var first = new RegistryPackAssetResolver(jdbc, new ObjectMapper(), manager, clock::get);
        var second = new RegistryPackAssetResolver(jdbc, new ObjectMapper(), manager, clock::get);
        assertThat(first.snapshot().assets()).isEmpty(); assertThat(second.snapshot().assets()).isEmpty();
        registry.install(pack("1.0.0"), "admin"); registry.activate("fixture", "1.0.0", "admin");
        clock.addAndGet(java.util.concurrent.TimeUnit.SECONDS.toNanos(10));
        assertThat(first.resolve("ontology","fixture").orElseThrow().packVersion()).isEqualTo("1.0.0");
        assertThat(second.snapshot()).isEqualTo(first.snapshot());
        registry.install(pack("1.0.1"), "admin"); registry.activate("fixture", "1.0.1", "admin");
        clock.addAndGet(java.util.concurrent.TimeUnit.SECONDS.toNanos(10));
        assertThat(second.resolve("ontology","fixture").orElseThrow().packVersion()).isEqualTo("1.0.1");
        registry.rollback("fixture", "admin"); clock.addAndGet(java.util.concurrent.TimeUnit.SECONDS.toNanos(10));
        assertThat(first.resolve("ontology","fixture").orElseThrow().packVersion()).isEqualTo("1.0.0");
        assertThat(second.snapshot()).isEqualTo(first.snapshot());
    }

    @Test void migrationRollbackAndReapplyWorkOnEmptyRegistry() throws Exception {
        try (var connection = dataSource.getConnection()) {
            connection.setSchema("copilot_ai");
            try (var liquibase = new Liquibase("config/liquibase/pack-registry-test.xml",
                    new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
                liquibase.rollback(3, "");
                assertThat(jdbc.queryForObject("SELECT to_regclass('copilot_ai.studio_pack')",String.class)).isNull();
                liquibase.update("");
            }
        }
        assertThat(registry.install(pack("1.0.0"),"admin").created()).isTrue();
    }

    private PackArchiveValidator.ValidatedPack templates(String version, String code, String sql) throws Exception {
        var mapper = new ObjectMapper();
        var manifest = mapper.readTree("""
                {"apiVersion":"dts.pack/v1","name":"fixture","version":"1.0.0","vendor":"fixture",
                 "requires":{"dts-studio":">=1.0.0"},"datasources":[{"ref":"prs-mart"}],
                 "capabilities":{"templates":[{"path":"templates.json","schemaVersion":1}]}}
                """);
        ((com.fasterxml.jackson.databind.node.ObjectNode)manifest).put("version", version);
        var template = mapper.createObjectNode().put("id",code).put("domain","fixture").put("sql",sql).put("datasource_ref","prs-mart");
        template.put("match_order", Integer.parseInt(version.substring(version.lastIndexOf('.') + 1)));
        template.putArray("question_patterns").add("fixture"); template.putArray("question_samples").add("fixture"); template.putObject("params");
        var content = mapper.createObjectNode(); content.putArray("templates").add(template);
        return new PackArchiveValidator().validate(new ByteArrayInputStream(PackArchiveValidatorTest.archive(
                java.util.Map.of("pack-manifest.yaml",mapper.writeValueAsBytes(manifest),"templates.json",mapper.writeValueAsBytes(content)),true)));
    }

    @Test void templatesSwitchRollbackAndAuditFailureAreAtomicAndPreserveManualContent() throws Exception {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM copilot_ai.nl2sql_query_template WHERE source='legacy-liquibase'",Integer.class)).isEqualTo(57);
        jdbc.update("INSERT INTO copilot_ai.nl2sql_query_template(template_code,domain,intent_patterns,question_samples,sql_template) VALUES ('MANUAL','fixture','[]','[]','select 42')");
        registry.install(templates("1.0.0","TPL-02","select 1"),"admin"); registry.activate("fixture","1.0.0","admin");
        registry.install(templates("1.0.1","TPL-02","select 2"),"admin");
        jdbc.execute("ALTER TABLE copilot_ai.studio_audit_outbox ADD CONSTRAINT reject_activation CHECK (event->>'subject' <> 'fixture@1.0.1' OR event->>'type' <> 'dts.pack.activated')");
        assertThatThrownBy(() -> registry.activate("fixture","1.0.1","admin")).isInstanceOf(RuntimeException.class);
        assertThat(templateSql("TPL-02")).isEqualTo("select 1");
        jdbc.execute("ALTER TABLE copilot_ai.studio_audit_outbox DROP CONSTRAINT reject_activation");
        registry.activate("fixture","1.0.1","admin"); assertThat(templateSql("TPL-02")).isEqualTo("select 2");
        assertThat(jdbc.queryForObject("SELECT match_order FROM copilot_ai.nl2sql_query_template WHERE template_code='TPL-02'", Integer.class)).isEqualTo(1);
        registry.rollback("fixture","admin"); assertThat(templateSql("TPL-02")).isEqualTo("select 1");
        assertThat(jdbc.queryForObject("SELECT match_order FROM copilot_ai.nl2sql_query_template WHERE template_code='TPL-02'", Integer.class)).isZero();
        assertThat(templateSql("MANUAL")).isEqualTo("select 42");
        registry.install(templates("1.0.2","MANUAL","select 0"),"admin");
        assertThatThrownBy(() -> registry.activate("fixture","1.0.2","admin"))
                .isInstanceOfSatisfying(PackException.class,e -> assertThat(e.code()).isEqualTo("PACK_TEMPLATE_CONFLICT"));
        assertThat(templateSql("MANUAL")).isEqualTo("select 42");
        assertThat(templateSql("TPL-02")).isEqualTo("select 1");
    }

    @Test void migrationDoesNotClaimEditedHistoricalTemplate() throws Exception {
        try(var connection = dataSource.getConnection()) {
            connection.setSchema("copilot_ai");
            try(var liquibase=new Liquibase("config/liquibase/pack-registry-test.xml",new ClassLoaderResourceAccessor(),new JdbcConnection(connection))) {
                liquibase.rollback(2, "");
                jdbc.update("UPDATE copilot_ai.nl2sql_query_template SET sql_template='select 42' WHERE template_code='TPL-02'");
                liquibase.update("");
            }
        }
        assertThat(jdbc.queryForObject("SELECT source FROM copilot_ai.nl2sql_query_template WHERE template_code='TPL-02'",String.class)).isNull();
        registry.install(templates("1.0.0","TPL-02","select 1"),"admin");
        assertThatThrownBy(() -> registry.activate("fixture","1.0.0","admin")).isInstanceOf(PackException.class);
        assertThat(templateSql("TPL-02")).isEqualTo("select 42");
    }

    private String templateSql(String code) {
        return jdbc.queryForObject("SELECT sql_template FROM copilot_ai.nl2sql_query_template WHERE template_code=?",String.class,code);
    }

    @Test void rejectsConflictingPackAndVersion() throws Exception {
        registry.install(pack("1.0.0"),"admin");
        var conflicting=new PackArchiveValidator().validate(new ByteArrayInputStream(PackArchiveValidatorTest.pack("other","1.0.0",
                "{\"domain\":\"fixture\",\"objects\":[]}")));
        assertThatThrownBy(() -> registry.install(conflicting,"admin"))
                .isInstanceOfSatisfying(PackException.class,e -> assertThat(e.code()).isEqualTo("PACK_DOMAIN_CONFLICT"));
        var changed=new PackArchiveValidator().validate(new ByteArrayInputStream(PackArchiveValidatorTest.pack("fixture","1.0.0",
                "{\"domain\":\"fixture\",\"objects\":[],\"description\":\"changed\"}")));
        assertThatThrownBy(() -> registry.install(changed,"admin"))
                .isInstanceOfSatisfying(PackException.class,e -> assertThat(e.code()).isEqualTo("PACK_VERSION_CONFLICT"));
    }
}
