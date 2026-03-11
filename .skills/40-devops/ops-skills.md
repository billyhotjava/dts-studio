# DevOps & Operations Skills

Status: PLACEHOLDER — to be implemented in dts-agent + dts-operator

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| ops/check-health | Rule | Check health status of all DTS services | P0 |
| ops/diagnose-error | LLM | Analyze error logs and suggest fix actions | P1 |
| ops/scale-service | Workflow | Scale service replicas with approval | P1 |
| ops/backup-database | Rule | Trigger database backup to MinIO | P0 |
| ops/install-pack | Workflow | Install AppPack with admin approval | P0 |
| ops/upgrade-pack | Workflow | Upgrade AppPack with compatibility check | P1 |
| ops/generate-report | Hybrid | Generate system health/usage report | P1 |

## Implementation Notes

- check-health: queries K8s API via dts-operator for pod status, readiness probes.
- diagnose-error: reads Loki logs + Tempo traces, LLM identifies root cause.
- scale-service / install-pack / upgrade-pack: admin approval required via dts-workflow.
- backup-database: triggers pg_dump / clickhouse-backup, stores in MinIO.
