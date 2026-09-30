package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.LinkedHashMap;
import java.util.Map;

/** Template projection participates in the caller's activation transaction, including audit. */
public final class TemplateSyncService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public TemplateSyncService(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc=jdbc; this.mapper=mapper; }

    public void activate(long packId, long versionId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Template activation requires a transaction");
        Map<String,JsonNode> templates = new LinkedHashMap<>();
        for(String content:jdbc.queryForList("SELECT content::text FROM copilot_ai.studio_pack_asset WHERE pack_version_id=? AND kind='templates'",String.class,versionId)) {
            try {
                for(JsonNode template:mapper.readTree(content).path("templates")) {
                    if(templates.putIfAbsent(template.path("id").asText(),template)!=null)
                        throw new PackException(409,"PACK_TEMPLATE_CONFLICT","Duplicate template ID");
                }
            } catch (java.io.IOException e) { throw new IllegalStateException("Invalid installed templates",e); }
        }
        for(String code:templates.keySet()) {
            Integer conflicts=jdbc.queryForObject("""
                    SELECT count(*) FROM copilot_ai.nl2sql_query_template t
                    LEFT JOIN copilot_ai.studio_pack_version v ON v.id=t.source_pack_version_id
                    WHERE t.template_code=? AND coalesce(v.pack_id,0)<>? AND coalesce(t.source,'manual')<>'legacy-liquibase'
                    """,Integer.class,code,packId);
            if(conflicts!=null && conflicts>0) throw new PackException(409,"PACK_TEMPLATE_CONFLICT","Template belongs to another owner: "+code);
        }
        jdbc.update("DELETE FROM copilot_ai.nl2sql_query_template WHERE source_pack_version_id IN (SELECT id FROM copilot_ai.studio_pack_version WHERE pack_id=?)",packId);
        for(var entry:templates.entrySet()) {
            JsonNode t=entry.getValue();
            int written = jdbc.update("""
                    INSERT INTO copilot_ai.nl2sql_query_template AS existing
                    (template_code,domain,role_hint,intent_patterns,question_samples,sql_template,parameters,target_view,description,priority,is_active,source,source_pack_version_id,datasource_ref,match_order)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,'pack',?,?,?)
                    ON CONFLICT(template_code) DO UPDATE SET domain=excluded.domain,role_hint=excluded.role_hint,
                    intent_patterns=excluded.intent_patterns,question_samples=excluded.question_samples,sql_template=excluded.sql_template,
                    parameters=excluded.parameters,target_view=excluded.target_view,description=excluded.description,priority=excluded.priority,
                    is_active=excluded.is_active,datasource_ref=excluded.datasource_ref,source=excluded.source,source_pack_version_id=excluded.source_pack_version_id,match_order=excluded.match_order,updated_at=now()
                    WHERE existing.source='legacy-liquibase'
                    """, entry.getKey(),t.path("domain").asText(),nullable(t,"role_hint"), t.path("question_patterns").toString(),
                    t.path("question_samples").toString(),t.path("sql").asText(),t.path("params").toString(),nullable(t,"target_view"),
                    nullable(t,"description"),t.path("priority").asInt(),t.path("is_active").asBoolean(true),versionId,t.path("datasource_ref").asText(),
                    t.hasNonNull("match_order") ? t.path("match_order").intValue() : null);
            if (written != 1) throw new PackException(409,"PACK_TEMPLATE_CONFLICT","Template ownership changed during activation");
        }
    }

    private static String nullable(JsonNode node,String field) { return node.path(field).isNull() || node.path(field).isMissingNode() ? null : node.path(field).asText(); }
}
