package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Replay, never approximate SQL with text extraction. Only accepts a fresh disposable database. */
public final class TemplateExportCli {
    private TemplateExportCli() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"studio_test".equals(System.getenv("PG_DB"))
                || !"127.0.0.1".equals(System.getenv("PG_HOST"))) {
            throw new IllegalArgumentException("Use with-test-postgres.sh and supply a new output JSON path");
        }
        var source = new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:%s/studio_test".formatted(System.getenv("PG_PORT")),
                System.getenv("PG_USER"),System.getenv("PG_PASSWORD"));
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE SCHEMA copilot_ai");
        try(var connection=source.getConnection()) {
            connection.setSchema("copilot_ai");
            try(var liquibase=new Liquibase("config/liquibase/template-export-runtime.xml",new ClassLoaderResourceAccessor(),new JdbcConnection(connection))) {
                liquibase.update("");
            }
        }
        // Freeze the legacy matcher's observed tie order into data, never infer it from template IDs.
        var matchOrder = new java.util.LinkedHashMap<String,Integer>();
        for (String code : jdbc.queryForList("SELECT template_code FROM copilot_ai.nl2sql_query_template "
                + "WHERE is_active=true ORDER BY priority DESC", String.class)) {
            matchOrder.put(code, matchOrder.size());
        }
        var templates=jdbc.queryForList("""
                SELECT template_code AS id,domain,role_hint,intent_patterns::json AS question_patterns,
                       question_samples::json AS question_samples,sql_template AS sql,parameters::json AS params,
                       target_view,description,priority,is_active,'prs-mart' AS datasource_ref,'1' AS version,
                       md5((to_jsonb(t)-'id'-'created_at'-'updated_at'-'source'-'source_pack_version_id'-'datasource_ref')::text) AS legacy_fingerprint
                FROM copilot_ai.nl2sql_query_template t ORDER BY template_code
                """);
        ObjectMapper mapper=new ObjectMapper();
        for(var template:templates) {
            // Inactive templates remain exportable; append them after the observed active sequence.
            template.put("match_order", matchOrder.computeIfAbsent(template.get("id").toString(),
                    ignored -> matchOrder.size()));
            for(String key:java.util.List.of("question_patterns","question_samples","params")) {
                Object value=template.get(key); template.put(key,value==null?mapper.createObjectNode():mapper.readTree(value.toString()));
            }
        }
        Files.writeString(Path.of(args[0]),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("templates",templates))+"\n",
                java.nio.file.StandardOpenOption.CREATE_NEW);
        System.out.println("Exported "+templates.size()+" templates after ordered migration replay");
    }
}
