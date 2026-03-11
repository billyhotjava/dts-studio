# Platform Data Skills

Status: PLACEHOLDER — to be implemented in dts-agent

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| data/schema-lookup | Rule | Look up table/column metadata from Ontology | P0 |
| data/execute-query | Rule | Execute SQL query via dts-query-service → dts-data-security | P0 |
| data/preview-data | Rule | Preview first N rows of a data source | P0 |
| data/profile-data | Hybrid | Generate data profiling statistics | P1 |
| data/import-csv | Rule | Import CSV/Excel into Ontology instance | P1 |
| data/export-data | Rule | Export query results to CSV/Excel/JSON | P1 |

## Implementation Notes

- All data Skills route through dts-data-security for permission and masking.
- execute-query requires SQL sandbox (no unguarded DELETE/UPDATE/DROP).
- profile-data uses LLM for anomaly explanation, rules for statistics.
