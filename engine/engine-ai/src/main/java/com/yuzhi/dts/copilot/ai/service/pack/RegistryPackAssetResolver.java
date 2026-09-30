package com.yuzhi.dts.copilot.ai.service.pack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Reads generation and assets from one MVCC snapshot; caches at most ten seconds. */
@Service
public class RegistryPackAssetResolver implements PackAssetResolver {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate transaction;
    private volatile Snapshot current;
    private volatile long refreshAt;
    private final java.util.function.LongSupplier clock;

    @org.springframework.beans.factory.annotation.Autowired
    public RegistryPackAssetResolver(JdbcTemplate jdbc, ObjectMapper mapper, PlatformTransactionManager manager) {
        this(jdbc, mapper, manager, System::nanoTime);
    }

    RegistryPackAssetResolver(JdbcTemplate jdbc, ObjectMapper mapper, PlatformTransactionManager manager,
                              java.util.function.LongSupplier clock) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.clock = clock;
        transaction = new TransactionTemplate(manager);
        transaction.setReadOnly(true);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(10);
    }

    @Override
    public synchronized Snapshot snapshot() {
        if (current != null && clock.getAsLong() < refreshAt) return current;
        Snapshot fresh = transaction.execute(tx -> {
            Long generation = jdbc.queryForObject("SELECT generation FROM copilot_ai.studio_pack_generation WHERE id=1", Long.class);
            if (current != null && generation != null && current.generation() == generation) return current;
            var assets = jdbc.query("""
                    SELECT p.name,v.version,a.kind,a.key,a.content_text,a.content::text
                    FROM copilot_ai.studio_pack_asset a
                    JOIN copilot_ai.studio_pack_version v ON v.id=a.pack_version_id
                    JOIN copilot_ai.studio_pack p ON p.id=v.pack_id
                    WHERE v.status='ACTIVE' AND a.tenant_id IS NULL ORDER BY p.name,a.kind,a.key
                    """, (rs, n) -> {
                try {
                    String json = rs.getString(6);
                    return new Asset(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), json == null ? null : mapper.readTree(json));
                } catch (JsonProcessingException e) { throw new IllegalStateException("Invalid registry asset", e); }
            });
            return new Snapshot(java.util.Objects.requireNonNull(generation), assets);
        });
        current = java.util.Objects.requireNonNull(fresh);
        refreshAt = clock.getAsLong() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        return current;
    }
}
