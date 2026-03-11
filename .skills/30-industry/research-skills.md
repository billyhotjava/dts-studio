# Research Institute Skills

Status: PLACEHOLDER — to be implemented in research-pack

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| research/track-milestone | Hybrid | Track project milestones and flag at-risk items | P0 |
| research/analyze-quality | LLM | Analyze quality inspection data, identify worst-performing processes | P0 |
| research/initiate-ncr-review | Workflow | Initiate NCR closure with five-step review process | P0 |
| research/compare-batches | LLM | Compare material batch quality across suppliers | P1 |
| research/generate-test-report | Hybrid | Generate test report from experimental data | P1 |
| research/bom-impact-analysis | Rule | Analyze BOM change impact across products | P1 |

## Domain Constraints (from .rules/50-appstack/industry/research.rules)

- NCR closure follows five-step procedure (五步法归零).
- All quality actions: HITL required, no auto-approve.
- Data retention: minimum 10 years.
