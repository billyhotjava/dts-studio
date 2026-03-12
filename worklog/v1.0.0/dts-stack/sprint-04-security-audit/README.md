# Sprint 4: Security & Audit (Java)

**Weeks:** 7-8
**Theme:** Iron Law 3 (data security) + Iron Law 4 (audit) + HITL workflow
**Story Points:** 21

## Tasks

| # | Task | Points | Status |
|---|------|--------|--------|
| 4.1 | dts-data-security — Data exit checkpoint (Iron Law 3) | 8 | TODO |
| 4.2 | dts-audit-log — Immutable audit log (Iron Law 4) | 5 | TODO |
| 4.3 | dts-workflow — HITL approval engine | 8 | TODO |

## Dependencies
- S3: dts-ontology-store (classification data), dts-query-service (data access)

## Critical Notes
- dts-data-security is mandatory for ALL data exits including AI queries
- dts-audit-log is append-only — NO UPDATE/DELETE methods
- dts-workflow supports risk-level-based approval routing
