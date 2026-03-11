# Task 12.2: Helm Charts

**Sprint:** 12 — Integration | **Points:** 5 | **Status:** TODO

## Responsibility
- Umbrella Helm chart `dts` with all 25 components
- Sub-charts for middleware (PG, Kafka, ClickHouse, Neo4j, MinIO, Keycloak)
- `values.yaml` (defaults) + `values-offline.yaml` (air-gapped overrides)
- Online vs offline: ONLY LLM endpoint + image registry differ

## Steps
1. Create Chart.yaml with dependencies
2. Create templates for all Java services
3. Create templates for all Python services
4. Create templates for Go operator
5. Create templates for frontend apps (nginx)
6. Create values.yaml and values-offline.yaml
7. Verify with `helm template` and `helm install --dry-run`
8. Commit
