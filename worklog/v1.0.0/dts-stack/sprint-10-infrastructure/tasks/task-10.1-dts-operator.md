# Task 10.1: dts-operator — K8s Operator

**Sprint:** 10 — Infrastructure | **Points:** 13 | **Status:** TODO

## Responsibility
- CRD: DtsCluster — manages all DTS components lifecycle
- CRD: AppPack — manages Pack install/upgrade/uninstall
- Component health monitoring + auto-recovery
- Middleware provisioning (PG, Kafka, ClickHouse, Neo4j, MinIO)
- Rolling upgrade orchestration

## Steps
1. `operator-sdk init` + create DtsCluster + AppPack APIs
2. Implement DtsCluster controller (deploy/update all components) → tests
3. Implement AppPack controller (install/upgrade/uninstall) → tests
4. Implement health monitoring → tests
5. Implement middleware provisioning → tests
6. E2E test with kind cluster
7. Commit
