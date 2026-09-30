package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Registry writes and their audit records commit together. Activation is globally serialized. */
@Service
public class PackRegistryService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ObjectMapper mapper;
    private final TemplateSyncService templates;

    public PackRegistryService(JdbcTemplate jdbc, PlatformTransactionManager transactions, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setTimeout(30);
        this.mapper = mapper;
        this.templates = new TemplateSyncService(jdbc, mapper);
    }

    public record Version(long packId, long versionId, String name, String version, String status,
                          String checksum, String installedAt, String activatedAt) {}
    public record Installed(Version version, boolean created, List<String> warnings) {}
    public record Activated(String name, String version, String status, String previousVersion, long generation) {}

    public Installed install(PackArchiveValidator.ValidatedPack pack, String actor) {
        requireActor(actor);
        return transaction.execute(tx -> {
            lockGeneration();
            List<Version> previous = versions(pack.name(), pack.version());
            if (!previous.isEmpty()) {
                if (!previous.getFirst().checksum().equals(pack.checksum()))
                    throw conflict("PACK_VERSION_CONFLICT");
                return new Installed(previous.getFirst(), false, pack.warnings());
            }
            List<String> vendors = jdbc.queryForList("SELECT vendor FROM copilot_ai.studio_pack WHERE name=?", String.class, pack.name());
            if (!vendors.isEmpty() && !vendors.getFirst().equals(pack.vendor())) throw conflict("PACK_VENDOR_CONFLICT");
            checkCollisions(pack);
            jdbc.update("INSERT INTO copilot_ai.studio_pack(name,vendor) VALUES (?,?) ON CONFLICT(name) DO NOTHING",
                    pack.name(), pack.vendor());
            Long packId = jdbc.queryForObject("SELECT id FROM copilot_ai.studio_pack WHERE name=?", Long.class, pack.name());
            Long versionId = jdbc.queryForObject("""
                    INSERT INTO copilot_ai.studio_pack_version(pack_id,version,status,manifest,checksum,installed_by)
                    VALUES (?,?,'INSTALLED',CAST(? AS jsonb),?,?) RETURNING id
                    """, Long.class, packId, pack.version(), pack.manifest().toString(), pack.checksum(), actor);
            for (var asset : pack.assets()) {
                jdbc.update("""
                        INSERT INTO copilot_ai.studio_pack_asset(pack_version_id,kind,key,path,content,content_text,sha256)
                        VALUES (?,?,?,?,CAST(? AS jsonb),?,?)
                        """, versionId, asset.kind(), asset.key(), asset.path(),
                        asset.json() == null ? null : asset.json().toString(), asset.text(), asset.sha256());
            }
            audit("installed", pack.name(), pack.version(), actor);
            return new Installed(versions(pack.name(), pack.version()).getFirst(), true, pack.warnings());
        });
    }

    private void checkCollisions(PackArchiveValidator.ValidatedPack pack) {
        var domains = new java.util.HashSet<String>();
        for (var asset : pack.assets()) {
            if (asset.kind().equals("ontology")) {
                String domain = asset.json().path("domain").asText();
                if (!domains.add(domain)) throw conflict("PACK_DOMAIN_CONFLICT");
                Integer collisions = jdbc.queryForObject("""
                        SELECT count(*) FROM copilot_ai.studio_pack_asset a
                        JOIN copilot_ai.studio_pack_version v ON v.id=a.pack_version_id
                        JOIN copilot_ai.studio_pack p ON p.id=v.pack_id
                        WHERE p.name<>? AND a.kind='ontology' AND a.content->>'domain'=?
                        """, Integer.class, pack.name(), domain);
                if (collisions != null && collisions > 0) throw conflict("PACK_DOMAIN_CONFLICT");
            }
            Integer collisions = jdbc.queryForObject("""
                    SELECT count(*) FROM copilot_ai.studio_pack_asset a
                    JOIN copilot_ai.studio_pack_version v ON v.id=a.pack_version_id
                    JOIN copilot_ai.studio_pack p ON p.id=v.pack_id
                    WHERE p.name<>? AND a.kind=? AND a.key=?
                    """, Integer.class, pack.name(), asset.kind(), asset.key());
            if (collisions != null && collisions > 0) throw conflict("PACK_ASSET_CONFLICT");
        }
    }

    public Activated activate(String name, String version, String actor) {
        requireActor(actor);
        return transaction.execute(tx -> {
            long generation = lockGeneration();
            return switchVersion(name, version, actor, generation, "activated");
        });
    }

    public Activated rollback(String name, String actor) {
        requireActor(actor);
        return transaction.execute(tx -> {
            long generation = lockGeneration();
            List<String> targets = jdbc.queryForList("""
                    SELECT v.version FROM copilot_ai.studio_pack_version v
                    JOIN copilot_ai.studio_pack p ON p.id=v.pack_id
                    WHERE p.name=? AND v.status='SUPERSEDED'
                    ORDER BY v.activation_generation DESC, v.id DESC LIMIT 1
                    """, String.class, name);
            if (targets.isEmpty()) throw conflict("NO_ROLLBACK_TARGET");
            return switchVersion(name, targets.getFirst(), actor, generation, "rolledback");
        });
    }

    private Activated switchVersion(String name, String version, String actor, long generation, String action) {
        List<Version> targets = versions(name, version);
        if (targets.isEmpty()) throw new PackException(404, "PACK_NOT_FOUND", "Pack version not found");
        Version target = targets.getFirst();
        if (target.status().equals("ACTIVE")) throw conflict("PACK_ALREADY_ACTIVE");
        if (target.status().equals("FAILED")) throw conflict("PACK_VERSION_FAILED");
        List<String> previous = jdbc.queryForList("SELECT version FROM copilot_ai.studio_pack_version WHERE pack_id=? AND status='ACTIVE'",
                String.class, target.packId());
        templates.activate(target.packId(), target.versionId());
        jdbc.update("UPDATE copilot_ai.studio_pack_version SET status='SUPERSEDED' WHERE pack_id=? AND status='ACTIVE'", target.packId());
        jdbc.update("UPDATE copilot_ai.studio_pack_version SET status='ACTIVE', activated_at=now(), activation_generation=? WHERE id=?",
                generation + 1, target.versionId());
        jdbc.update("UPDATE copilot_ai.studio_pack_generation SET generation=? WHERE id=1", generation + 1);
        audit(action, name, version, actor);
        return new Activated(name, version, "ACTIVE", previous.isEmpty() ? null : previous.getFirst(), generation + 1);
    }

    public List<Version> versions(String name, String version) {
        return jdbc.query("""
                SELECT p.id pack_id,v.id version_id,p.name,v.version,v.status,v.checksum,
                       v.installed_at::text,v.activated_at::text
                FROM copilot_ai.studio_pack p JOIN copilot_ai.studio_pack_version v ON p.id=v.pack_id
                WHERE (CAST(? AS text) IS NULL OR p.name=?) AND (CAST(? AS text) IS NULL OR v.version=?)
                ORDER BY p.name,v.installed_at DESC,v.id DESC
                """, (rs, n) -> new Version(rs.getLong("pack_id"), rs.getLong("version_id"), rs.getString("name"),
                rs.getString("version"), rs.getString("status"), rs.getString("checksum"),
                rs.getString("installed_at"), rs.getString("activated_at")), name, name, version, version);
    }

    public List<Map<String, Object>> assets(String name, String version, String kind) {
        if (versions(name, version).isEmpty()) throw new PackException(404, "PACK_NOT_FOUND", "Pack version not found");
        return jdbc.queryForList("""
                SELECT a.kind,a.key,a.path,a.sha256 FROM copilot_ai.studio_pack_asset a
                JOIN copilot_ai.studio_pack_version v ON v.id=a.pack_version_id
                JOIN copilot_ai.studio_pack p ON p.id=v.pack_id
                WHERE p.name=? AND v.version=? AND (CAST(? AS text) IS NULL OR a.kind=?) AND a.tenant_id IS NULL
                ORDER BY a.kind,a.key
                """, name, version, kind, kind);
    }

    private long lockGeneration() {
        return Objects.requireNonNull(jdbc.queryForObject(
                "SELECT generation FROM copilot_ai.studio_pack_generation WHERE id=1 FOR UPDATE", Long.class));
    }

    private void audit(String action, String name, String version, String actor) {
        UUID id = UUID.randomUUID();
        JsonNode event = mapper.valueToTree(Map.of("specversion", "1.0", "id", id.toString(),
                "source", "dts-studio/engine-ai", "type", "dts.pack." + action,
                "time", Instant.now().toString(), "subject", name + "@" + version,
                "actorid", actor, "data", Map.of("name", name, "version", version)));
        jdbc.update("INSERT INTO copilot_ai.studio_audit_outbox(id,event) VALUES (?,CAST(? AS jsonb))", id, event.toString());
    }

    private static void requireActor(String actor) {
        if (actor == null || actor.isBlank() || actor.length() > 128)
            throw new PackException(403, "PACK_FORBIDDEN", "Authenticated pack administrator required");
    }

    private static PackException conflict(String code) { return new PackException(409, code, code); }
}
