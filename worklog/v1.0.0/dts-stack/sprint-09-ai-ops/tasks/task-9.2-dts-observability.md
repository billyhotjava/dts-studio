# Task 9.2: dts-observability — AI Trace Trees

**Sprint:** 9 — AI Ops | **Points:** 5 | **Status:** TODO

## Responsibility
- AI execution trace visualization (intent → plan → skills → results)
- Agent chain tracing (link OTel traces with AI decision traces)
- Token cost tracking per tenant / user / skill
- Latency analysis per skill
- Grafana dashboard integration

## Steps
1. Implement trace tree builder (from audit events) → tests
2. Implement token cost aggregator → tests
3. Implement latency analysis → tests
4. Create Grafana dashboard JSON
5. gRPC server
6. Commit
