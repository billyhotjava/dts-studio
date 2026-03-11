# Platform Query Skills

Status: PLACEHOLDER — to be implemented in dts-query-ai

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| query/nl2sql | LLM | Natural language to SQL translation | P0 |
| query/explain-result | LLM | Explain query results in natural language | P0 |
| query/suggest-chart | LLM | Suggest chart type for given data | P1 |
| query/generate-chart | Hybrid | Generate ECharts config from data + intent | P0 |

## Implementation Notes

- nl2sql: uses Ontology metadata as context for accurate table/column resolution.
- SQL output validated by SQL sandbox before execution.
- explain-result: summarizes data patterns, outliers, trends in natural language.
- generate-chart: rule-based chart type selection + LLM for title/annotation.
