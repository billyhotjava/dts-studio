# Task 1.5: Dev Environment — Docker Compose Middleware

**Sprint:** 1 — Foundation
**Points:** 3
**Status:** TODO

## Files

- Create: `dts-stack/deploy/dev/docker-compose.yml`
- Create: `dts-stack/deploy/dev/.env.example`

## Step 1: Create docker-compose.yml

All middleware for local development:
- PostgreSQL 16 (port 5432)
- Kafka KRaft 3.7 (port 9092)
- ClickHouse 24.3 (port 8123, 9000)
- Neo4j 5 Community (port 7474, 7687)
- MinIO (port 9010, 9011)
- Keycloak 25 (port 8080)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: dts
      POSTGRES_USER: dts
      POSTGRES_PASSWORD: ${PG_PASSWORD:-dts_dev}
    volumes:
      - pg_data:/var/lib/postgresql/data

  kafka:
    image: apache/kafka:3.7.0
    ports: ["9092:9092"]
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      CLUSTER_ID: dts-dev-cluster-001

  clickhouse:
    image: clickhouse/clickhouse-server:24.3
    ports: ["8123:8123", "9000:9000"]

  neo4j:
    image: neo4j:5-community
    ports: ["7474:7474", "7687:7687"]
    environment:
      NEO4J_AUTH: neo4j/${NEO4J_PASSWORD:-dts_dev}

  minio:
    image: minio/minio:latest
    ports: ["9010:9000", "9011:9001"]
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: dts
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD:-dts_dev123}

  keycloak:
    image: quay.io/keycloak/keycloak:25.0
    ports: ["8080:8080"]
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: ${KC_PASSWORD:-admin}

volumes:
  pg_data:
```

## Step 2: Verify

Run: `cd dts-stack/deploy/dev && docker compose up -d`
Expected: All 6 services running

Run: `docker compose ps`
Expected: All services healthy/running

## Step 3: Commit

```bash
git add dts-stack/deploy/dev/
git commit -m "feat(s1): add dev docker-compose with all middleware"
```
