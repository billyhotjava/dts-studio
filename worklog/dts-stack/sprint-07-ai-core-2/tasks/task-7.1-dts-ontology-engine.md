# Task 7.1: dts-ontology-engine — Semantic Reasoning

**Sprint:** 7 — AI Core 2 | **Points:** 10 | **Status:** TODO

## Responsibility
- NL → Ontology mapping (user query → ObjectType + properties)
- Vector indexing of Ontology definitions (pgvector)
- Semantic search across Ontology
- KnowHow extraction: expert conversation → knowledge fragments → draft KnowHow
- KnowHow → Skill conversion pipeline

## Steps
1. Implement vector indexing of ObjectTypes → tests
2. Implement NL → Ontology mapping → tests
3. Implement KnowHow extraction (LLM-powered) → tests
4. Implement KnowHow → Skill draft conversion → tests
5. gRPC server
6. Eval dataset + baseline accuracy
7. Commit
