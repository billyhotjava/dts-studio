# Task 4.2: dts-audit-log — Immutable Audit Log

**Sprint:** 4 — Security & Audit
**Points:** 5
**Status:** TODO
**Iron Law:** #4 — Full Audit Trail

## Responsibility
- Kafka consumer: `dts.audit.events` topic → ClickHouse storage
- CloudEvents format (specversion 1.0)
- Query API: by trace_id, user, time range, event_type
- Trace aggregator: group events by trace_id for full decision chain
- **APPEND-ONLY**: No UPDATE/DELETE operations exist

## ClickHouse Schema

```sql
CREATE TABLE audit_events (
    event_id String,
    event_type String,
    source String,
    user_id String,
    tenant_id String,
    trace_id String,
    timestamp DateTime64(3),
    data String,
    risk_level String
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (tenant_id, timestamp, trace_id)
TTL timestamp + INTERVAL 3 YEAR;
```

## Steps

1. Write Kafka consumer tests → implement
2. Write query API tests → implement
3. Write trace aggregator tests → implement
4. Verify NO UPDATE/DELETE methods exist in codebase
5. Integration tests with Testcontainers (Kafka + ClickHouse)
6. Commit

```bash
git commit -m "feat(s4): implement dts-audit-log — Iron Law 4 immutable audit"
```
