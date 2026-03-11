# Platform Ontology Skills

Status: PLACEHOLDER — to be implemented in dts-agent + dts-ontology-engine

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| ontology/search | LLM | Semantic search across Ontology types and instances | P0 |
| ontology/suggest-type | LLM | Suggest ObjectType definition from sample data | P1 |
| ontology/map-source | LLM | Suggest field mapping between data source and ObjectType | P1 |
| ontology/lineage-query | Rule | Query data lineage graph via Neo4j | P0 |

## Implementation Notes

- ontology/search uses pgvector for semantic matching + Ontology metadata for filtering.
- suggest-type analyzes column names, types, sample values to propose ObjectType schema.
- lineage-query traverses Neo4j graph, results filtered by user permissions.
