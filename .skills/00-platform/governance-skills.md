# Platform Governance Skills

Status: PLACEHOLDER — to be implemented in dts-data-quality + dts-governance

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| governance/classify-column | LLM | Auto-classify column sensitivity level | P1 |
| governance/detect-anomaly | Hybrid | Detect data quality anomalies | P0 |
| governance/suggest-rule | LLM | Suggest quality rules from data patterns | P1 |
| governance/catalog-search | LLM | Search data asset catalog with NL | P1 |

## Implementation Notes

- classify-column: LLM analyzes column name + sample values → suggests classification (public/internal/confidential/top-secret).
- detect-anomaly: rule engine for statistical checks + LLM for explaining anomalies.
- suggest-rule: analyzes data distribution and proposes completeness/consistency/range rules.
