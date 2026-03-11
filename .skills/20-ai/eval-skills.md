# AI Evaluation Skills

Status: PLACEHOLDER — to be implemented in dts-ai-eval

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| eval/run-suite | Rule | Execute evaluation suite against target Skill | P0 |
| eval/compare-versions | Rule | Compare accuracy/cost between two Skill versions | P0 |
| eval/generate-cases | LLM | Auto-generate evaluation cases from Ontology + sample data | P1 |
| eval/analyze-bad-cases | LLM | Cluster and analyze failed cases for root cause | P1 |

## Implementation Notes

- run-suite: batch invokes target Skill with evaluation dataset, computes accuracy/latency/cost metrics.
- compare-versions: runs same suite against two versions, produces comparison report.
- generate-cases: uses Ontology schema + sample data to create diverse test cases.
- analyze-bad-cases: clusters failures by error type, suggests improvement areas.
