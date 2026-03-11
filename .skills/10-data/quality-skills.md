# Data Quality Skills

Status: PLACEHOLDER — to be implemented in dts-data-quality

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| quality/run-rules | Rule | Execute quality rules against a dataset | P0 |
| quality/detect-drift | Hybrid | Detect data distribution drift over time | P1 |
| quality/suggest-fix | LLM | Suggest fix actions for quality issues | P1 |
| quality/auto-clean | Hybrid | Apply automated data cleaning (dedup, format normalization) | P2 |

## Implementation Notes

- run-rules: executes SQL-based quality checks, reports pass/fail per rule.
- detect-drift: statistical comparison of current vs historical distributions + LLM explanation.
- suggest-fix: given quality issue, LLM proposes corrective action or query.
- auto-clean: rule-based cleaning with LLM confirmation for ambiguous cases.
