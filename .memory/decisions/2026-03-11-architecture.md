# Architecture Design Decisions — 2026-03-11

## Decision 1: Full Rebuild (not incremental migration)
- **Context**: v2.4.1 exists with Java monolith + React frontend
- **Decision**: Abandon all existing code, redesign from scratch
- **Rationale**: Historical baggage prevents AI-native architecture; clean slate is faster than migration
- **User quote**: "可以抛弃之前dts的理念,包括源代码,我们可以重新设计"

## Decision 2: Three-Language Architecture
- **Options**: A) Python only, B) Java only, C) Python + Java + Go
- **Decision**: C — Python (AI) + Java (Business CRUD) + Go (K8s Infrastructure)
- **Rationale**: Team has 10+ years Java, Python best for AI ecosystem, Go native for K8s
- **User quote**: "面向AI的部分全部使用python,平台还有大量的curd操作用java更好,基础设施全部是k8s用go比较方便"

## Decision 3: Communication — gRPC + Kafka Hybrid
- **Options**: A) Event-driven Kafka, B) API-first gRPC, C) Hybrid
- **Decision**: C refined — gRPC for control plane, Kafka for data plane
- **Rationale**: Online/offline unified (no degraded mode), supports CDC + time-series + audit
- **Constraint**: User insisted on single architecture, no two-mode maintenance

## Decision 4: Storage — PG + ClickHouse + Neo4j + pgvector + MinIO
- **Decision**: Full stack, all deployed in K8s
- **Graph DB**: Neo4j CE to start, abstract interface for NebulaGraph switch
- **Vector DB**: pgvector to start, abstract interface for Milvus switch
- **Rationale**: POC fast with lighter options, switch when scale demands

## Decision 5: Frontend — React + shadcn + Tailwind (unchanged)
- **Options**: A) Keep stack, B) Rewrite same stack, C) New stack
- **Decision**: B — Full rewrite, same tech stack, add Vercel AI SDK + React Flow + @dnd-kit
- **Rationale**: No frontend framework better than React for AI ecosystem. Vite SPA suits offline nginx deployment.

## Decision 6: Three-App Frontend (not unified)
- **Decision**: Keep admin / cortex / pilot separate
- **Rationale**: Different user roles (admin=ops, cortex=analyst, pilot=decision-maker)
- **User quote**: "三端是根据角色来的"

## Decision 7: Deployment — All-in-K8s, No Degradation
- **Decision**: Single architecture for online and offline, middleware all internal by default
- **Constraint**: Support external instances via Helm values
- **User quote**: "尽可能离线和在线保持相当的模式,不想去维护两套模式"

## Decision 8: Keycloak + JWT
- **Decision**: Continue using Keycloak, JWT propagation between services
- **Rationale**: Not a differentiator, proven solution, government-friendly
