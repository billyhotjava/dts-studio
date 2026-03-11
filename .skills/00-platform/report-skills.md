# Platform Report Skills

Status: PLACEHOLDER — to be implemented in dts-agent

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| report/generate | Hybrid | Generate report from template + data | P1 |
| report/summarize | LLM | Summarize dataset or dashboard in NL | P0 |
| report/schedule | Rule | Schedule periodic report generation | P1 |

## Implementation Notes

- generate: fills template with queried data + LLM-generated insights/conclusions.
- summarize: input is query results or dashboard data, output is executive summary text.
- schedule: registers a cron job in dts-scheduler to run report/generate periodically.
