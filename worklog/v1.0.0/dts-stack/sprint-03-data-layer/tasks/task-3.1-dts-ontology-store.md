# Task 3.1: dts-ontology-store — Ontology CRUD

**Sprint:** 3 — Data Layer
**Points:** 10
**Status:** TODO

## Responsibility
- ObjectType CRUD (PG JSONB for flexible properties)
- Instance CRUD (PG with GIN index on properties)
- Relationship management (Neo4j for graph traversal)
- Metric definitions
- Action definitions with risk levels

## DB Schema

```sql
CREATE TABLE object_types (
    name VARCHAR(100) NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    description TEXT,
    properties JSONB NOT NULL DEFAULT '[]',
    actions JSONB DEFAULT '[]',
    version INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (namespace, name)
);

CREATE TABLE instances (
    id VARCHAR(36) PRIMARY KEY,
    object_type VARCHAR(100) NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    FOREIGN KEY (namespace, object_type) REFERENCES object_types(namespace, name)
);
CREATE INDEX idx_instances_type ON instances(namespace, object_type);
CREATE INDEX idx_instances_tenant ON instances(tenant_id);
CREATE INDEX idx_instances_props ON instances USING GIN (properties);

CREATE TABLE metrics (
    name VARCHAR(100) NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    description TEXT,
    formula TEXT,
    unit VARCHAR(50),
    frequency VARCHAR(50),
    thresholds JSONB,
    dependencies JSONB DEFAULT '[]',
    PRIMARY KEY (namespace, name)
);
```

## Neo4j — Relationship storage

```cypher
CREATE (a:ObjectType {name: $from, namespace: $ns})
       -[:RELATES_TO {type: $relType, cardinality: $card}]->
       (b:ObjectType {name: $to, namespace: $ns})
```

## Steps

1. Create project skeleton
2. Write Flyway migration
3. Write ObjectType CRUD tests → implement
4. Write Instance CRUD tests → implement
5. Write Neo4j relationship tests → implement
6. Write Metric CRUD tests → implement
7. Implement OntologyGrpcService
8. Integration tests with Testcontainers (PG + Neo4j)
9. Commit

```bash
git commit -m "feat(s3): implement dts-ontology-store with CRUD + Neo4j relationships"
```
