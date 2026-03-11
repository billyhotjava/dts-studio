# DTS AI Decision OS — Architecture Design Document

**Date**: 2026-03-11
**Status**: APPROVED
**Authors**: Billy + Claude

## 1. Product Vision

DTS (Decision Twins System) is an AI-driven Enterprise Decision Operating System.

**Core formula**: Ontology (data foundation) + Intent (understanding) + Agent (execution)

**Core value**: Turn expert tacit knowledge into executable capability packs that can be scaled across the organization. One expert × AI extraction × system scale = 500x output.

## 2. Five Iron Laws

See: `/.rules/five-iron-laws.rules`

1. **Human Override** — Turn off AI, system still works. Every step has manual alternative.
2. **Security Cannot Be Bypassed** — All requests through gateway, no exceptions.
3. **Data Security Is DNA** — AI sees only authorized data. All data exits through security checkpoint.
4. **Full Audit Trail** — Append-only, immutable logs for all human and AI operations.
5. **Capability Before Interface** — Working API + tests before any UI work.

## 3. Three-Language Architecture

| Language | Responsibility | Team |
|----------|---------------|------|
| Python | AI Core (Agent/Intent/RAG/Skill/Eval/Scheduling) | Senior engineers |
| Java | Business Platform (CRUD/RBAC/Keycloak/Storage/Governance) | Junior/mid engineers |
| Go | Infrastructure (K8s Operator/CLI/Deployment) | General engineers |

**Key constraint**: Python services are enhancement layers. Java services must function independently.

## 4. Communication Architecture

### Control Plane (Synchronous): gRPC
- Java ↔ Python: skill invocation, permission checks, queries
- Java ↔ Go: deployment commands, status queries
- Frontend → Java: REST via dts-gateway

### Data Plane (Asynchronous): Kafka KRaft
- Data sources → Ontology: CDC, batch imports
- Agent event streams: progress, decision steps → SSE to frontend
- Audit logs: all operation records
- Pack data channels: Pack push data to platform

## 5. Storage Layer

| Storage | Purpose | Notes |
|---------|---------|-------|
| PostgreSQL | Ontology metadata, business data, permissions | Primary relational store |
| ClickHouse | Time-series, analytical queries (MES/SCADA) | High-volume analysis |
| Neo4j CE | Graph relationships (lineage, impact chains) | Interface abstraction → NebulaGraph |
| pgvector | Vector index (RAG, semantic search) | Interface abstraction → Milvus |
| MinIO | Object storage (files, models, bundles) | S3-compatible |
| Kafka KRaft | Messaging, event streaming, CDC | No ZooKeeper |

## 6. Service Inventory (25 Components)

### Python Layer (10 — AI Core, all degradable)

| # | Service | Responsibility |
|---|---------|---------------|
| 1 | dts-agent | Agent runtime (LangGraph), multi-step reasoning, Persona |
| 2 | dts-intent-engine | NL → structured Intent, routing |
| 3 | dts-ontology-engine | Semantic reasoning, vector indexing, NL→Ontology mapping |
| 4 | dts-data-connector | Multi-source ingestion, CDC, format conversion |
| 5 | dts-data-quality | AI data quality detection, anomaly discovery |
| 6 | dts-query-ai | NL2SQL, query suggestions |
| 7 | dts-ai-eval | Evaluation suites, regression gates, Bad Case flywheel |
| 8 | dts-observability | AI Trace trees, Agent chain tracing, cost statistics |
| 9 | dts-scheduler | Task scheduling DAG, scheduled Skills, backfill, SLA |
| 10 | dts-perception (P2) | Multi-modal input (voice/vision) |

### Java Layer (10 — Business Platform, independently functional)

| # | Service | Responsibility |
|---|---------|---------------|
| 1 | dts-platform | Users/roles/permissions/tenants/menus/config/Keycloak |
| 2 | dts-gateway | API gateway, auth enforcement, gRPC-Web |
| 3 | dts-ontology-store | Ontology CRUD (types/instances/relations/metrics/rules/actions) |
| 4 | dts-query-service | SQL query execution, data source routing |
| 5 | dts-governance | Metadata management, data lineage, classification |
| 6 | dts-asset | Data asset catalog, search, sharing |
| 7 | dts-data-security | Dynamic masking, row/column permissions, watermarking |
| 8 | dts-workflow | Approval engine (HITL, access requests, change approvals) |
| 9 | dts-audit-log | Audit logs (append-only, immutable) |
| 10 | dts-data-service | Data API publishing, Token/quota, push/subscribe |

### Go Layer (2 — Infrastructure)

| # | Service | Responsibility |
|---|---------|---------------|
| 1 | dts-operator | K8s Operator, component lifecycle, AppPack deployment |
| 2 | dts-cli | CLI install/upgrade/backup/offline-bundle/Pack mgmt |

### Frontend (3 — React 19 + Vite + shadcn + Tailwind)

| # | App | Role | Additional Libraries |
|---|-----|------|---------------------|
| 1 | dts-admin-webapp | System admin / ops | — |
| 2 | dts-cortex-webapp | Technical analysts | React Flow |
| 3 | dts-pilot-webapp | Decision makers | Vercel AI SDK, @dnd-kit |

## 7. AppPack Protocol

See: `/.rules/apppack-protocol.rules`

Pack manifest defines 10 capability types:
1. ontology — schema definitions
2. skills — AI skills (gRPC)
3. connectors — data source adapters
4. frontend — UI modules
5. workflows — approval templates
6. persona — industry persona (prompt + skills + scheduled tasks)
7. metrics — metric definitions
8. quality_rules — data quality rules
9. actions — business actions with risk levels
10. evaluations — evaluation datasets

Lifecycle managed by dts-operator. All Pack access through dts-gateway.

## 8. Authentication

- **Keycloak** for identity (OIDC/SAML/PKI)
- **JWT propagation** between services via gRPC metadata
- **dts-gateway** enforces authentication on all external requests
- No mTLS (JWT sufficient for internal service trust)

## 9. Observability

- **OpenTelemetry** SDK in all services (Python/Java/Go)
- **Grafana Tempo** for distributed tracing
- **Prometheus** for metrics
- **Grafana Loki** for logs
- **Grafana** for unified dashboards
- **traceId** links infrastructure traces + AI execution traces

## 10. Deployment

- **All-in-K8s**: unified architecture, no online/offline divergence
- Middleware (PG/Kafka/ClickHouse/Neo4j/MinIO) deployed inside K8s by default
- External instances supported via Helm values
- Online vs offline: ONLY LLM endpoint + image registry differ
- Four scenarios: dev single-node / small offline / mid-size cluster / cloud

## 11. Competitive Positioning

### Advantages
- Ontology + Intent dual-layer model (beyond Palantir's Ontology + Workflow)
- Five Iron Laws = enterprise trust for AI-driven decisions
- Offline deployment capability for government/defense
- Know-how flywheel: knowledge accumulates, switching cost = losing core competitiveness

### Future-Proofing
- AI Agent as primary execution engine (not optional enhancement)
- Multi-modal readiness (dts-perception reserved)
- Agent Protocol for AI-to-AI communication
- Plugin/Skill hot-loading for rapid AI capability iteration
- Interface abstraction on storage layers (graph DB, vector DB) for technology evolution

## 12. Coverage Mapping — Eight Functional Centers

| Original Center | New Services |
|----------------|-------------|
| Data Ingestion Center | dts-data-connector |
| Data Development Center | dts-query-service, dts-query-ai, dts-scheduler |
| Data Governance Center | dts-governance, dts-data-quality |
| Data Asset Portal | dts-asset, dts-ontology-engine |
| Task Operations Center | dts-scheduler, dts-observability |
| Data Service Center | dts-data-service |
| Data Visualization Center | dts-pilot-webapp (@dnd-kit, React Flow) |
| AI Intelligence Workbench | dts-agent, dts-intent-engine, dts-ai-eval |
