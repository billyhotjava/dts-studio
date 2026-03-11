# Task 5.1: dts-governance — Metadata Management & Lineage

**Sprint:** 5 — Governance & Assets | **Points:** 8 | **Status:** TODO

## Responsibility
- Metadata management (tagging, categorization, business glossary)
- Data lineage tracking (Neo4j graph: DataSource → ObjectType → Metric → Skill)
- Data classification management (assign classification to columns)
- Impact analysis (trace downstream effects of schema changes)

## Neo4j Lineage Model
```cypher
(:DataSource)-[:FEEDS]->(:ObjectType)-[:COMPUTES]->(:Metric)
(:ObjectType)-[:USED_BY]->(:Skill)
(:Skill)-[:PRODUCES]->(:ObjectType)
```

## Steps
1. Design + implement lineage graph model → tests
2. Implement metadata tagging → tests
3. Implement impact analysis (given ObjectType change, what's affected?) → tests
4. Implement GovernanceGrpcService
5. Integration tests
6. Commit
