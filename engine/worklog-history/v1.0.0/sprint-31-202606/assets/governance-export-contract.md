# Governance Caliber Export Contract

**Sprint**: 31
**Status**: implemented locally, live dts-platform endpoint pluggable
**Producer**: governance source of truth
**Current provider**: `LocalGovernanceCaliberExportProvider`
**Consumer**: `CaliberGuardrailSyncService` / semantic pack generated guardrails

## Purpose

This contract exports caliber guardrails as a versioned, hash-addressed artifact so copilot packs can be generated and drift-checked from a single source of truth.

## Shape

```json
{
  "version": "local-governance/caliber-rules.v1.json",
  "contentHash": "sha256:<stable-content-hash>",
  "guardrailsByDomain": {
    "finance": [
      "[CAL-SETTLEMENT-CHAIN] 标签: settlement_chain; 规则: ..."
    ],
    "procurement": [
      "[CAL-INVENTORY-COST] 标签: inventory_cost_sku; 规则: ..."
    ]
  }
}
```

## Fields

| Field | Required | Description |
|-------|----------|-------------|
| `version` | yes | Export version or artifact identity. Current local value is `local-governance/caliber-rules.v1.json`. |
| `contentHash` | yes | Stable SHA-256 hash over version, domain keys, and ordered guardrail text. Same content must produce the same hash. |
| `guardrailsByDomain` | yes | Domain-keyed generated guardrails. Keys are normalized lowercase `CatalogDomain` values. |

## Domain Filtering

Consumers may request one or more domains. The provider must return only requested domains and recompute `contentHash` for that filtered export.

Current covered domains:

- `finance`
- `procurement`

## Current Implementation

- Java contract: `CaliberGuardrailSyncService.GovernanceCaliberExport`
- Provider SPI: `CaliberGuardrailSyncService.GovernanceCaliberExportProvider`
- Local provider: `LocalGovernanceCaliberExportProvider`
- Source artifact: `governance/caliber-rules.v1.json`
- Pack projection: `semantic-packs/* generatedGuardrails`
- Health evidence: `caliberGuardrailSync` details expose `exportVersion` and `exportHash`

## Verification

- `LocalGovernanceCaliberExportProviderTest`: stable hash and domain filtering.
- `SemanticPackGovernanceGuardrailTest`: pack generated guardrails match registry output.
- `test_sprint31_pack_governance_guardrails.sh`: repeatable IT gate for contract + pack projection.
