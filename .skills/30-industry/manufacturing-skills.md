# Manufacturing Industry Skills

Status: PLACEHOLDER — to be implemented in manufacturing-pack

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| mfg/calculate-oee | Rule | Calculate OEE from availability, performance, quality data | P0 |
| mfg/predict-maintenance | LLM | Predict equipment failure from SCADA trend + maintenance history | P1 |
| mfg/optimize-schedule | LLM | Suggest production schedule optimization | P1 |
| mfg/detect-quality-drift | Hybrid | Detect quality drift from inspection time-series | P0 |
| mfg/create-hold-order | Workflow | Create quality hold order with QA manager approval | P0 |
| mfg/root-cause-analysis | LLM | Analyze defect root cause from multi-dimensional production data | P1 |

## Domain Constraints (from .rules/50-appstack/industry/manufacturing.rules)

- Machine start/stop: risk_level high, mandatory HITL.
- Quality hold/release: QA manager approval required.
- SCADA data: high-frequency, use ClickHouse for queries.
