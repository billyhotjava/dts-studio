# DTS v3.0 AI Decision OS — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the complete DTS AI Decision OS with 25 microservices + 3 frontends across 12 Sprints (24 weeks).

**Architecture:** Three-language (Python/Java/Go) microservice system communicating via gRPC (control) + Kafka (data). Java layer must function independently (Iron Law 1). All data exits through dts-data-security (Iron Law 3). All operations audited via Kafka (Iron Law 4).

**Tech Stack:**
- Java 21 + Spring Boot 3 + gRPC + Flyway + PostgreSQL
- Python 3.12 + FastAPI + LangGraph + gRPC
- Go 1.22 + K8s Operator SDK + Cobra CLI
- React 19 + Vite + TypeScript + shadcn/ui + Tailwind
- Kafka KRaft + ClickHouse + Neo4j CE + pgvector + MinIO + Keycloak

**Sprint Cadence:** 2 weeks per Sprint, 12 Sprints total

**Existing Codebase:** dts-stack has legacy Java services (dts-platform, dts-admin) and 3 frontends (v2.x). All will be rewritten from scratch per architecture decision.

---

## Sprint Overview

| Sprint | Weeks | Theme | Services |
|--------|-------|-------|----------|
| S1 | 1-2 | Foundation & Skeleton | Monorepo setup, proto definitions, shared libs |
| S2 | 3-4 | Core Platform (Java) | dts-gateway, dts-platform |
| S3 | 5-6 | Data Layer (Java) | dts-ontology-store, dts-query-service |
| S4 | 7-8 | Security & Audit (Java) | dts-data-security, dts-audit-log, dts-workflow |
| S5 | 9-10 | Governance & Assets (Java) | dts-governance, dts-asset, dts-data-service |
| S6 | 11-12 | AI Core 1 (Python) | dts-agent, dts-intent-engine |
| S7 | 13-14 | AI Core 2 (Python) | dts-ontology-engine, dts-query-ai |
| S8 | 15-16 | AI Data (Python) | dts-data-connector, dts-data-quality, dts-scheduler |
| S9 | 17-18 | AI Ops (Python) | dts-ai-eval, dts-observability |
| S10 | 19-20 | Infrastructure (Go) | dts-operator, dts-cli |
| S11 | 21-22 | Frontend | dts-admin-webapp, dts-cortex-webapp, dts-pilot-webapp |
| S12 | 23-24 | Integration & Release | E2E tests, Helm charts, offline bundle, release |

---

## Sprint 1: Foundation & Skeleton (Week 1-2)

### Task 1.1: Java Monorepo — Parent POM & Shared Module

**Files:**
- Create: `dts-stack/source/pom.xml` (parent POM)
- Create: `dts-stack/source/dts-common/pom.xml`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/config/GrpcServerConfig.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/config/KafkaConfig.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/security/JwtContextInterceptor.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/security/RequestContext.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/audit/AuditEventPublisher.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/exception/DtsException.java`
- Create: `dts-stack/source/dts-common/src/main/java/com/dts/common/exception/ErrorCode.java`

**Steps:**

**Step 1:** Create parent POM with Spring Boot 3.3, Java 21, module management

```xml
<!-- dts-stack/source/pom.xml -->
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
  </parent>
  <groupId>com.dts</groupId>
  <artifactId>dts-parent</artifactId>
  <version>3.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <properties>
    <java.version>21</java.version>
    <grpc.version>1.65.1</grpc.version>
    <protobuf.version>3.25.5</protobuf.version>
    <kafka.version>3.7.0</kafka.version>
    <flyway.version>10.15.0</flyway.version>
    <jooq.version>3.19.10</jooq.version>
    <google-java-format.version>1.22.0</google-java-format.version>
  </properties>

  <modules>
    <module>dts-common</module>
    <!-- Added per Sprint -->
  </modules>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-bom</artifactId>
        <version>${grpc.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

**Step 2:** Create dts-common module with RequestContext, JwtContextInterceptor, AuditEventPublisher

```java
// RequestContext.java — JWT claims propagated through gRPC metadata
public record RequestContext(
    String userId,
    String tenantId,
    String traceId,
    List<String> roles,
    List<String> permissions
) {
    public static final Context.Key<RequestContext> CTX_KEY =
        Context.key("request-context");
}
```

```java
// JwtContextInterceptor.java — gRPC server interceptor extracting JWT
public class JwtContextInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String jwt = headers.get(Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER));
        RequestContext ctx = parseJwt(jwt);
        Context context = Context.current().withValue(RequestContext.CTX_KEY, ctx);
        return Contexts.interceptCall(context, call, headers, next);
    }
}
```

```java
// AuditEventPublisher.java — Kafka-based audit event publishing
public class AuditEventPublisher {
    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private static final String TOPIC = "dts.audit.events";

    public void publish(String eventType, String source, Object data) {
        RequestContext ctx = RequestContext.CTX_KEY.get();
        AuditEvent event = new AuditEvent(
            UUID.randomUUID().toString(),
            eventType, source,
            ctx.userId(), ctx.tenantId(), ctx.traceId(),
            Instant.now(), data
        );
        kafkaTemplate.send(TOPIC, event.traceId(), event);
    }
}
```

**Step 3:** Run `mvn clean compile` — verify compilation

**Step 4:** Commit

```bash
git add dts-stack/source/pom.xml dts-stack/source/dts-common/
git commit -m "feat(s1): initialize Java monorepo with dts-common module"
```

---

### Task 1.2: Proto Definitions — DAP gRPC Contracts

**Files:**
- Create: `dts-stack/proto/dap/v1/common.proto`
- Create: `dts-stack/proto/dap/v1/ontology.proto`
- Create: `dts-stack/proto/dap/v1/skill.proto`
- Create: `dts-stack/proto/dap/v1/intent.proto`
- Create: `dts-stack/proto/dap/v1/security.proto`
- Create: `dts-stack/proto/dap/v1/audit.proto`
- Create: `dts-stack/proto/dap/v1/platform.proto`
- Create: `dts-stack/proto/buf.yaml`
- Create: `dts-stack/proto/buf.gen.yaml`

**Steps:**

**Step 1:** Create common.proto with shared types (RequestContext, Pagination, etc.)

```protobuf
// dts-stack/proto/dap/v1/common.proto
syntax = "proto3";
package dap.v1;
option java_package = "com.dts.proto.dap.v1";
option go_package = "github.com/billyhotjava/dts/proto/dap/v1";

message RequestContext {
  string user_id = 1;
  string tenant_id = 2;
  string trace_id = 3;
  string jwt_token = 4;
  repeated string permissions = 5;
}

message PageRequest {
  int32 page = 1;
  int32 size = 2;
  string sort_by = 3;
  bool ascending = 4;
}

message PageResponse {
  int32 total = 1;
  int32 page = 2;
  int32 size = 3;
}

message Timestamp {
  int64 seconds = 1;
  int32 nanos = 2;
}
```

**Step 2:** Create ontology.proto (ObjectType CRUD, Instance CRUD, Relationship CRUD)

```protobuf
// dts-stack/proto/dap/v1/ontology.proto
syntax = "proto3";
package dap.v1;
option java_package = "com.dts.proto.dap.v1";

import "dap/v1/common.proto";
import "google/protobuf/struct.proto";

service OntologyService {
  // ObjectType management
  rpc CreateObjectType(CreateObjectTypeRequest) returns (ObjectTypeResponse);
  rpc GetObjectType(GetObjectTypeRequest) returns (ObjectTypeResponse);
  rpc ListObjectTypes(ListObjectTypesRequest) returns (ListObjectTypesResponse);
  rpc UpdateObjectType(UpdateObjectTypeRequest) returns (ObjectTypeResponse);
  rpc DeleteObjectType(DeleteObjectTypeRequest) returns (DeleteObjectTypeResponse);

  // Instance management
  rpc CreateInstance(CreateInstanceRequest) returns (InstanceResponse);
  rpc GetInstance(GetInstanceRequest) returns (InstanceResponse);
  rpc QueryInstances(QueryInstancesRequest) returns (QueryInstancesResponse);

  // Relationship management
  rpc CreateRelationship(CreateRelationshipRequest) returns (RelationshipResponse);
  rpc GetRelationships(GetRelationshipsRequest) returns (GetRelationshipsResponse);
}

message ObjectType {
  string name = 1;
  string namespace = 2;
  string description = 3;
  repeated PropertyDef properties = 4;
  repeated ActionDef actions = 5;
}

message PropertyDef {
  string name = 1;
  string type = 2;       // string, number, enum, reference, object, array
  bool required = 3;
  string classification = 4;  // public, internal, confidential, restricted
  repeated string enum_values = 5;
  string source = 6;
  string unit = 7;
}

message ActionDef {
  string name = 1;
  string risk_level = 2;  // low, medium, high
  string description = 3;
}

message Instance {
  string id = 1;
  string object_type = 2;
  string namespace = 3;
  google.protobuf.Struct properties = 4;
  Timestamp created_at = 5;
  Timestamp updated_at = 6;
}
```

**Step 3:** Create skill.proto (SkillService — Execute/Describe/Validate)

```protobuf
// dts-stack/proto/dap/v1/skill.proto
syntax = "proto3";
package dap.v1;
option java_package = "com.dts.proto.dap.v1";

import "dap/v1/common.proto";
import "google/protobuf/struct.proto";

service SkillService {
  rpc Execute(SkillRequest) returns (stream SkillResponse);
  rpc Describe(DescribeRequest) returns (SkillDescriptor);
  rpc Validate(ValidateRequest) returns (ValidateResponse);
}

message SkillRequest {
  string skill_name = 1;
  string trace_id = 2;
  RequestContext context = 3;
  google.protobuf.Struct input = 4;
}

message SkillResponse {
  enum Status {
    RUNNING = 0;
    COMPLETED = 1;
    FAILED = 2;
    AWAITING_APPROVAL = 3;
  }
  Status status = 1;
  string step_description = 2;
  google.protobuf.Struct output = 3;
  AuditEntry audit = 4;
}

message AuditEntry {
  string event_type = 1;
  string source = 2;
  repeated string data_accessed = 3;
  int64 duration_ms = 4;
  int32 token_cost = 5;
  string risk_level = 6;
}

message SkillDescriptor {
  string name = 1;
  string namespace = 2;
  string description = 3;
  string type = 4;         // llm, rule, hybrid, workflow
  string risk_level = 5;
  repeated string permissions = 6;
  google.protobuf.Struct input_schema = 7;
  google.protobuf.Struct output_schema = 8;
}

message DescribeRequest {
  string skill_name = 1;
}

message ValidateRequest {
  string skill_name = 1;
  RequestContext context = 2;
  google.protobuf.Struct input = 3;
}

message ValidateResponse {
  bool valid = 1;
  repeated string errors = 2;
}
```

**Step 4:** Create intent.proto, security.proto, audit.proto, platform.proto

```protobuf
// intent.proto — Intent parsing and execution plan
service IntentService {
  rpc Parse(ParseIntentRequest) returns (IntentResponse);
  rpc ExecutePlan(ExecutePlanRequest) returns (stream PlanStepResponse);
}

// security.proto — Data security checks
service DataSecurityService {
  rpc CheckAccess(DataAccessRequest) returns (DataAccessResponse);
  rpc ApplyMasking(MaskingRequest) returns (MaskingResponse);
}

// audit.proto — Audit log queries
service AuditService {
  rpc QueryEvents(QueryAuditRequest) returns (QueryAuditResponse);
  rpc GetTraceEvents(GetTraceRequest) returns (GetTraceResponse);
}

// platform.proto — User/Tenant/Config management
service PlatformService {
  rpc GetCurrentUser(GetCurrentUserRequest) returns (UserResponse);
  rpc ListUsers(ListUsersRequest) returns (ListUsersResponse);
  rpc GetTenantConfig(GetTenantConfigRequest) returns (TenantConfigResponse);
}
```

**Step 5:** Create buf.yaml and buf.gen.yaml for code generation

```yaml
# buf.yaml
version: v2
modules:
  - path: .
lint:
  use:
    - DEFAULT
breaking:
  use:
    - FILE
```

```yaml
# buf.gen.yaml
version: v2
plugins:
  - remote: buf.build/protocolbuffers/java
    out: ../source/dts-common/src/main/java
  - remote: buf.build/grpc/java
    out: ../source/dts-common/src/main/java
  - remote: buf.build/protocolbuffers/python
    out: ../ai/dts-common-py/src/dts_proto
  - remote: buf.build/grpc/python
    out: ../ai/dts-common-py/src/dts_proto
```

**Step 6:** Run `buf lint` and `buf generate` — verify code generation

**Step 7:** Commit

```bash
git add dts-stack/proto/
git commit -m "feat(s1): define DAP v1 proto contracts"
```

---

### Task 1.3: Python Monorepo — Shared Library & Project Structure

**Files:**
- Create: `dts-stack/ai/pyproject.toml` (workspace root)
- Create: `dts-stack/ai/dts-common-py/pyproject.toml`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/__init__.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/config.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/grpc_utils.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/kafka_utils.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/security.py`
- Create: `dts-stack/ai/dts-common-py/tests/test_config.py`

**Steps:**

**Step 1:** Create workspace pyproject.toml with uv workspace

```toml
# dts-stack/ai/pyproject.toml
[project]
name = "dts-ai-workspace"
version = "3.0.0"
requires-python = ">=3.12"

[tool.uv.workspace]
members = ["dts-common-py", "dts-agent", "dts-intent-engine",
           "dts-ontology-engine", "dts-data-connector", "dts-data-quality",
           "dts-query-ai", "dts-ai-eval", "dts-observability", "dts-scheduler"]

[tool.ruff]
line-length = 100
target-version = "py312"

[tool.ruff.lint]
select = ["E", "F", "I", "N", "UP", "ASYNC"]
```

**Step 2:** Create dts-common-py with shared utilities

```python
# security.py — JWT context extraction for gRPC
from dataclasses import dataclass

@dataclass(frozen=True)
class RequestContext:
    user_id: str
    tenant_id: str
    trace_id: str
    roles: list[str]
    permissions: list[str]

def extract_context(grpc_context) -> RequestContext:
    """Extract RequestContext from gRPC metadata."""
    metadata = dict(grpc_context.invocation_metadata())
    # Parse JWT from authorization header
    jwt_token = metadata.get("authorization", "")
    claims = decode_jwt(jwt_token)
    return RequestContext(
        user_id=claims["sub"],
        tenant_id=claims["tenant_id"],
        trace_id=metadata.get("x-trace-id", str(uuid4())),
        roles=claims.get("roles", []),
        permissions=claims.get("permissions", []),
    )
```

**Step 3:** Write test, run `uv run pytest tests/ -v`

**Step 4:** Commit

```bash
git add dts-stack/ai/
git commit -m "feat(s1): initialize Python AI workspace with dts-common-py"
```

---

### Task 1.4: Go Module — Infrastructure Skeleton

**Files:**
- Create: `dts-stack/infra/go.mod`
- Create: `dts-stack/infra/go.sum`
- Create: `dts-stack/infra/cmd/operator/main.go`
- Create: `dts-stack/infra/cmd/cli/main.go`
- Create: `dts-stack/infra/internal/common/version.go`

**Steps:**

**Step 1:** Initialize Go module

```bash
cd dts-stack/infra && go mod init github.com/billyhotjava/dts/infra
```

**Step 2:** Create operator and CLI entry points (skeleton only)

```go
// cmd/operator/main.go
package main

import (
    "fmt"
    "os"
)

func main() {
    fmt.Println("dts-operator v3.0.0")
    os.Exit(0)
}
```

**Step 3:** Run `go build ./...` — verify compilation

**Step 4:** Commit

```bash
git add dts-stack/infra/
git commit -m "feat(s1): initialize Go infrastructure module"
```

---

### Task 1.5: Dev Environment — Docker Compose for Middleware

**Files:**
- Create: `dts-stack/deploy/dev/docker-compose.yml`
- Create: `dts-stack/deploy/dev/.env.example`

**Steps:**

**Step 1:** Create docker-compose.yml with all middleware dependencies

```yaml
# PostgreSQL, Kafka KRaft, ClickHouse, Neo4j CE, MinIO, Keycloak, Grafana
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

**Step 2:** Run `docker compose up -d` — verify all services start

**Step 3:** Commit

```bash
git add dts-stack/deploy/dev/
git commit -m "feat(s1): add dev docker-compose with all middleware"
```

---

### Task 1.6: CI Pipeline — GitHub Actions

**Files:**
- Create: `dts-stack/.github/workflows/ci.yml`

**Steps:**

**Step 1:** Create CI workflow with Java/Python/Go build + test

```yaml
name: DTS CI
on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [develop]

jobs:
  java:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: cd source && mvn clean verify -T1C

  python:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: astral-sh/setup-uv@v3
      - run: cd ai && uv sync && uv run ruff check . && uv run pytest

  go:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-go@v5
        with: { go-version: '1.22' }
      - run: cd infra && go build ./... && go test ./...

  proto:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: bufbuild/buf-action@v1
        with:
          input: proto
```

**Step 2:** Commit

```bash
git add dts-stack/.github/
git commit -m "feat(s1): add CI pipeline for Java/Python/Go/Proto"
```

---

## Sprint 2: Core Platform — Java (Week 3-4)

### Task 2.1: dts-gateway — API Gateway

**Files:**
- Create: `dts-stack/source/dts-gateway/pom.xml`
- Create: `src/main/java/com/dts/gateway/GatewayApplication.java`
- Create: `src/main/java/com/dts/gateway/config/SecurityConfig.java`
- Create: `src/main/java/com/dts/gateway/config/RouteConfig.java`
- Create: `src/main/java/com/dts/gateway/filter/JwtAuthFilter.java`
- Create: `src/main/java/com/dts/gateway/filter/RateLimitFilter.java`
- Create: `src/main/java/com/dts/gateway/filter/AuditFilter.java`
- Create: `src/main/java/com/dts/gateway/filter/GrpcWebFilter.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/dts/gateway/filter/JwtAuthFilterTest.java`
- Create: `Dockerfile`

**Steps:**

**Step 1:** Create Spring Cloud Gateway project with:
- JWT validation against Keycloak JWKS endpoint
- Route configuration for all backend services
- Rate limiting per tenant
- gRPC-Web translation for frontend→gRPC
- Audit filter publishing all requests to Kafka

**Step 2:** Write JwtAuthFilter tests (valid/invalid/expired JWT)

**Step 3:** Write RateLimitFilter tests

**Step 4:** Run `mvn clean test` — verify all pass

**Step 5:** Create Dockerfile (multi-stage, distroless base)

**Step 6:** Commit

```bash
git commit -m "feat(s2): implement dts-gateway with JWT auth, rate limiting, audit"
```

---

### Task 2.2: dts-platform — User/Tenant/Config Management

**Files:**
- Create: `dts-stack/source/dts-platform/` (Spring Boot 3 + gRPC + Flyway)
- Key classes:
  - `PlatformGrpcService.java` — implements PlatformService proto
  - `UserService.java` — CRUD + Keycloak sync
  - `TenantService.java` — tenant management
  - `ConfigService.java` — system configuration
  - `RbacService.java` — role-based access control
- DB migrations: `V1__init_platform.sql` (users, tenants, roles, permissions, configs)
- Tests: unit + integration (Testcontainers)

**Steps:**

**Step 1:** Create Flyway migration V1__init_platform.sql

```sql
CREATE TABLE tenants (
    tenant_id VARCHAR(36) PRIMARY KEY,
    tenant_name VARCHAR(100) NOT NULL,
    plan VARCHAR(20) DEFAULT 'standard',
    config JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) REFERENCES tenants(tenant_id),
    username VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(200),
    email VARCHAR(200),
    keycloak_id VARCHAR(36),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE roles (
    role_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) REFERENCES tenants(tenant_id),
    role_name VARCHAR(100) NOT NULL,
    permissions TEXT[] DEFAULT '{}'
);

CREATE TABLE user_roles (
    user_id VARCHAR(36) REFERENCES users(user_id),
    role_id VARCHAR(36) REFERENCES roles(role_id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE system_configs (
    config_key VARCHAR(200) PRIMARY KEY,
    config_value JSONB NOT NULL,
    tenant_id VARCHAR(36),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

**Step 2:** Implement PlatformGrpcService + services

**Step 3:** Write unit tests for UserService, TenantService, RbacService

**Step 4:** Write integration test with Testcontainers (PG + Keycloak)

**Step 5:** Run `mvn clean verify`

**Step 6:** Commit

```bash
git commit -m "feat(s2): implement dts-platform with user/tenant/rbac/config"
```

---

## Sprint 3: Data Layer — Java (Week 5-6)

### Task 3.1: dts-ontology-store — Ontology CRUD

**Files:**
- Create: `dts-stack/source/dts-ontology-store/`
- Key classes:
  - `OntologyGrpcService.java` — implements OntologyService proto
  - `ObjectTypeRepository.java` — ObjectType CRUD (PG JSONB)
  - `InstanceRepository.java` — Instance CRUD
  - `RelationshipRepository.java` — Relationship management (Neo4j)
  - `MetricRepository.java` — Metric definitions
- DB: `V1__init_ontology.sql` (object_types, instances, metrics, actions)
- Neo4j: relationship storage and traversal
- Tests: unit + integration

**Steps:**

**Step 1:** Design schema — object_types table with JSONB for flexible properties

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

**Step 2:** Implement OntologyGrpcService with CRUD operations

**Step 3:** Implement Neo4j relationship storage with Cypher queries

**Step 4:** Write tests (CRUD, query by properties, relationship traversal)

**Step 5:** Commit

```bash
git commit -m "feat(s3): implement dts-ontology-store with CRUD + Neo4j relationships"
```

---

### Task 3.2: dts-query-service — SQL Query Execution

**Files:**
- Create: `dts-stack/source/dts-query-service/`
- Key classes:
  - `QueryGrpcService.java` — query execution endpoint
  - `DataSourceRouter.java` — routes to PG/ClickHouse based on query type
  - `QueryValidator.java` — SQL injection prevention, scope checking
  - `ResultFormatter.java` — unified result format
- Integration with dts-data-security for permission checks
- Tests: query routing, validation, result formatting

**Steps:**

**Step 1:** Create service with data source routing logic

```java
public class DataSourceRouter {
    // Route to appropriate data source based on query characteristics
    public DataSource route(String sql, String namespace) {
        if (isTimeSeriesQuery(sql) || isAggregationQuery(sql)) {
            return clickHouseDataSource;
        }
        return postgresDataSource;
    }
}
```

**Step 2:** Implement query validation (SQL whitelist, tenant isolation)

**Step 3:** Write tests

**Step 4:** Commit

```bash
git commit -m "feat(s3): implement dts-query-service with PG/CH routing"
```

---

## Sprint 4: Security & Audit — Java (Week 7-8)

### Task 4.1: dts-data-security — Data Exit Checkpoint (Iron Law 3)

**Files:**
- Create: `dts-stack/source/dts-data-security/`
- Key classes:
  - `DataSecurityGrpcService.java` — implements DataSecurityService proto
  - `ColumnPolicyEngine.java` — per-column allow/mask/deny decisions
  - `RowFilterEngine.java` — generates row-level filter SQL
  - `MaskingEngine.java` — dynamic data masking (email, phone, ID card)
  - `WatermarkService.java` — data watermarking for export tracing
  - `ClassificationRegistry.java` — data classification management
- Tests: masking rules, policy evaluation, row filter generation

**Critical:** This service is Iron Law 3 — ALL data exits must pass through it, including AI queries.

**Steps:**

**Step 1:** Design policy evaluation engine

```java
public record ColumnPolicy(String column, PolicyAction action, MaskingRule maskingRule) {}

public enum PolicyAction { ALLOW, MASK, DENY }

public class ColumnPolicyEngine {
    public List<ColumnPolicy> evaluate(
            String userId, String tenantId, String objectType,
            List<String> requestedColumns) {
        // 1. Load classification for each column from ObjectType
        // 2. Load user's data access permissions
        // 3. Evaluate policy: classification × permission → action
        // 4. Return per-column decisions
    }
}
```

**Step 2:** Implement MaskingEngine (email → a***@b.com, phone → 138****0000)

**Step 3:** Implement RowFilterEngine (generates SQL WHERE clause for tenant isolation + row-level security)

**Step 4:** Write comprehensive tests (policy evaluation matrix, masking correctness)

**Step 5:** Commit

```bash
git commit -m "feat(s4): implement dts-data-security — Iron Law 3 data checkpoint"
```

---

### Task 4.2: dts-audit-log — Immutable Audit Log (Iron Law 4)

**Files:**
- Create: `dts-stack/source/dts-audit-log/`
- Key classes:
  - `AuditKafkaConsumer.java` — consumes from dts.audit.events topic
  - `AuditGrpcService.java` — implements AuditService proto (query only)
  - `AuditRepository.java` — append-only storage (ClickHouse)
  - `TraceAggregator.java` — aggregates events by trace_id

**Critical:** Append-only, no UPDATE/DELETE operations. Iron Law 4.

**Steps:**

**Step 1:** Create ClickHouse table for audit events

```sql
CREATE TABLE audit_events (
    event_id String,
    event_type String,
    source String,
    user_id String,
    tenant_id String,
    trace_id String,
    timestamp DateTime64(3),
    data String,  -- JSON
    risk_level String
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (tenant_id, timestamp, trace_id)
TTL timestamp + INTERVAL 3 YEAR;
```

**Step 2:** Implement Kafka consumer (CloudEvents format)

**Step 3:** Implement query API (by trace_id, by user, by time range, by event_type)

**Step 4:** Tests — verify no UPDATE/DELETE methods exist, verify query correctness

**Step 5:** Commit

```bash
git commit -m "feat(s4): implement dts-audit-log — Iron Law 4 immutable audit"
```

---

### Task 4.3: dts-workflow — Approval Engine (HITL)

**Files:**
- Create: `dts-stack/source/dts-workflow/`
- Key classes:
  - `WorkflowService.java` — approval workflow management
  - `ApprovalEngine.java` — HITL approval logic (pending/approved/rejected/expired)
  - `NotificationService.java` — notify approvers (SSE + Kafka)
  - `TimeoutScheduler.java` — auto-expire pending approvals

**Steps:**

**Step 1:** Create DB schema for workflows and approval requests

**Step 2:** Implement approval lifecycle (create → notify → decide → execute or reject)

**Step 3:** Implement timeout mechanism (configurable per risk level)

**Step 4:** Tests

**Step 5:** Commit

```bash
git commit -m "feat(s4): implement dts-workflow with HITL approval engine"
```

---

## Sprint 5: Governance & Assets — Java (Week 9-10)

### Task 5.1: dts-governance — Metadata Management & Lineage

**Files:**
- Create: `dts-stack/source/dts-governance/`
- Key features:
  - Metadata management (tagging, categorization)
  - Data lineage tracking (Neo4j-backed)
  - Data classification management
  - Impact analysis (trace downstream effects of changes)

**Steps:**

**Step 1:** Design lineage graph model in Neo4j

```cypher
// Lineage nodes and relationships
(:DataSource)-[:FEEDS]->(:ObjectType)-[:COMPUTES]->(:Metric)
(:ObjectType)-[:USED_BY]->(:Skill)
(:Skill)-[:PRODUCES]->(:ObjectType)
```

**Step 2:** Implement lineage tracking and impact analysis

**Step 3:** Tests

**Step 4:** Commit

```bash
git commit -m "feat(s5): implement dts-governance with lineage and classification"
```

---

### Task 5.2: dts-asset — Data Asset Catalog

**Files:**
- Create: `dts-stack/source/dts-asset/`
- Key features:
  - Asset catalog (searchable, taggable)
  - Asset sharing and access request workflows
  - Asset popularity tracking

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s5): implement dts-asset catalog with search and sharing"
```

---

### Task 5.3: dts-data-service — Data API Publishing

**Files:**
- Create: `dts-stack/source/dts-data-service/`
- Key features:
  - REST/gRPC API publishing from Ontology instances
  - Token/quota management per consumer
  - Push/subscribe for real-time data

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s5): implement dts-data-service with API publishing"
```

---

## Sprint 6: AI Core 1 — Python (Week 11-12)

### Task 6.1: dts-intent-engine — NL → Structured Intent

**Files:**
- Create: `dts-stack/ai/dts-intent-engine/`
- Key modules:
  - `server.py` — gRPC server (IntentService)
  - `parser.py` — NL → Intent structure (LLM-powered)
  - `router.py` — Intent → Skill routing
  - `prompt_templates/` — Intent parsing prompts
- Tests: intent parsing accuracy tests

**Steps:**

**Step 1:** Implement intent parsing with LLM

```python
# parser.py
async def parse_intent(raw_input: str, ontology_context: list[ObjectType]) -> Intent:
    """Parse natural language input into structured Intent."""
    prompt = INTENT_PARSE_PROMPT.format(
        input=raw_input,
        available_types=[t.name for t in ontology_context],
        available_metrics=[m.name for m in metrics],
    )
    response = await llm.generate(prompt, response_format=IntentSchema)
    return Intent(
        raw_input=raw_input,
        goals=response.goals,
        constraints=response.constraints,
    )
```

**Step 2:** Implement routing logic (map goals to skills)

**Step 3:** Write eval tests with sample intents

**Step 4:** Commit

```bash
git commit -m "feat(s6): implement dts-intent-engine with NL parsing"
```

---

### Task 6.2: dts-agent — Agent Runtime (LangGraph)

**Files:**
- Create: `dts-stack/ai/dts-agent/`
- Key modules:
  - `server.py` — gRPC server
  - `graph.py` — LangGraph agent graph definition
  - `planner.py` — Intent → ExecutionPlan
  - `executor.py` — Step-by-step plan execution
  - `persona.py` — Industry persona loading
  - `tools/` — Tool wrappers for Skill invocation
  - `memory/` — Conversation memory management

**Steps:**

**Step 1:** Design LangGraph agent graph

```python
# graph.py
from langgraph.graph import StateGraph, END

class AgentState(TypedDict):
    intent: Intent
    plan: ExecutionPlan
    current_step: int
    results: list[StepResult]
    needs_approval: bool
    messages: list[BaseMessage]

def build_agent_graph() -> StateGraph:
    graph = StateGraph(AgentState)
    graph.add_node("plan", planner_node)
    graph.add_node("execute_step", executor_node)
    graph.add_node("check_approval", approval_check_node)
    graph.add_node("wait_approval", approval_wait_node)
    graph.add_node("summarize", summary_node)

    graph.set_entry_point("plan")
    graph.add_edge("plan", "execute_step")
    graph.add_conditional_edges("execute_step", should_continue, {
        "next_step": "execute_step",
        "needs_approval": "check_approval",
        "done": "summarize",
        "error": "summarize",
    })
    graph.add_conditional_edges("check_approval", check_risk, {
        "auto_approve": "execute_step",
        "human_review": "wait_approval",
    })
    graph.add_edge("wait_approval", "execute_step")
    graph.add_edge("summarize", END)
    return graph.compile()
```

**Step 2:** Implement planner (Intent → multi-step plan)

**Step 3:** Implement executor (invoke Skills via gRPC, handle streaming)

**Step 4:** Implement persona loading (system prompt + skill preferences + ontology scope)

**Step 5:** Write tests (plan generation, execution flow, approval interception)

**Step 6:** Commit

```bash
git commit -m "feat(s6): implement dts-agent with LangGraph runtime"
```

---

## Sprint 7: AI Core 2 — Python (Week 13-14)

### Task 7.1: dts-ontology-engine — Semantic Reasoning

**Files:**
- Create: `dts-stack/ai/dts-ontology-engine/`
- Key features:
  - NL → Ontology mapping (user query → ObjectType + properties)
  - Vector indexing of Ontology (pgvector)
  - Semantic search across Ontology
  - KnowHow extraction and management

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s7): implement dts-ontology-engine with semantic reasoning"
```

---

### Task 7.2: dts-query-ai — NL2SQL + Query Intelligence

**Files:**
- Create: `dts-stack/ai/dts-query-ai/`
- Key features:
  - NL → SQL translation (context-aware, Ontology-informed)
  - Query suggestions and optimization hints
  - Result interpretation in natural language

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s7): implement dts-query-ai with NL2SQL"
```

---

## Sprint 8: AI Data — Python (Week 15-16)

### Task 8.1: dts-data-connector — Multi-Source Ingestion

**Files:**
- Create: `dts-stack/ai/dts-data-connector/`
- Key features:
  - JDBC connector (PG, MySQL, Oracle, SQL Server)
  - API connector (REST, GraphQL)
  - File connector (CSV, Excel, Parquet)
  - CDC support (Debezium via Kafka Connect)
  - OPC-UA connector (manufacturing)
  - MQTT connector (IoT)

**Steps:** Implement per connector type, test, commit.

```bash
git commit -m "feat(s8): implement dts-data-connector with multi-source ingestion"
```

---

### Task 8.2: dts-data-quality — AI Data Quality

**Files:**
- Create: `dts-stack/ai/dts-data-quality/`
- Key features:
  - Rule-based quality checks (completeness, uniqueness, range)
  - AI anomaly detection (statistical + ML-based)
  - Quality scoring and trending

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s8): implement dts-data-quality with anomaly detection"
```

---

### Task 8.3: dts-scheduler — Task Scheduling DAG

**Files:**
- Create: `dts-stack/ai/dts-scheduler/`
- Key features:
  - DAG-based task scheduling
  - Skill scheduling (cron-based)
  - Backfill support
  - SLA monitoring

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s8): implement dts-scheduler with DAG scheduling"
```

---

## Sprint 9: AI Ops — Python (Week 17-18)

### Task 9.1: dts-ai-eval — Evaluation Suites

**Files:**
- Create: `dts-stack/ai/dts-ai-eval/`
- Key features:
  - Evaluation suite management (JSONL datasets)
  - Regression gate (block deployment if accuracy drops)
  - Bad Case flywheel (collect failures → improve)
  - Accuracy tracking per Skill

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s9): implement dts-ai-eval with regression gates"
```

---

### Task 9.2: dts-observability — AI Trace Trees

**Files:**
- Create: `dts-stack/ai/dts-observability/`
- Key features:
  - AI execution trace visualization
  - Agent chain tracing (intent → plan → skills → results)
  - Token cost tracking per tenant/user/skill
  - Performance dashboards (Grafana integration)

**Steps:** Implement, test, commit.

```bash
git commit -m "feat(s9): implement dts-observability with AI trace trees"
```

---

## Sprint 10: Infrastructure — Go (Week 19-20)

### Task 10.1: dts-operator — K8s Operator

**Files:**
- Create: `dts-stack/infra/internal/operator/`
- Key features:
  - CRD: DtsCluster (manages all DTS components)
  - CRD: AppPack (manages Pack lifecycle: install/upgrade/uninstall)
  - Component lifecycle management
  - Health monitoring and auto-recovery
  - Middleware provisioning (PG, Kafka, etc.)

**Steps:**

**Step 1:** Initialize operator with Operator SDK

```bash
operator-sdk init --domain dts.io --repo github.com/billyhotjava/dts/infra
operator-sdk create api --group dts --version v1 --kind DtsCluster --resource --controller
operator-sdk create api --group dts --version v1 --kind AppPack --resource --controller
```

**Step 2:** Implement DtsCluster controller (deploy/update all components)

**Step 3:** Implement AppPack controller (install/upgrade/uninstall packs)

**Step 4:** Tests

**Step 5:** Commit

```bash
git commit -m "feat(s10): implement dts-operator with DtsCluster and AppPack CRDs"
```

---

### Task 10.2: dts-cli — Command Line Tool

**Files:**
- Create: `dts-stack/infra/internal/cli/`
- Key commands:
  - `dts install` — install DTS cluster
  - `dts upgrade` — upgrade DTS version
  - `dts backup/restore` — data backup and restore
  - `dts pack install/list/remove` — Pack management
  - `dts bundle` — create offline installation bundle
  - `dts status` — cluster health check

**Steps:**

**Step 1:** Implement using Cobra framework

```go
var rootCmd = &cobra.Command{
    Use:   "dts",
    Short: "DTS CLI — manage your Decision Twins System",
}

func init() {
    rootCmd.AddCommand(installCmd)
    rootCmd.AddCommand(upgradeCmd)
    rootCmd.AddCommand(backupCmd)
    rootCmd.AddCommand(restoreCmd)
    rootCmd.AddCommand(packCmd)
    rootCmd.AddCommand(bundleCmd)
    rootCmd.AddCommand(statusCmd)
}
```

**Step 2:** Implement each subcommand

**Step 3:** Tests

**Step 4:** Commit

```bash
git commit -m "feat(s10): implement dts-cli with install/upgrade/backup/pack commands"
```

---

## Sprint 11: Frontend (Week 21-22)

### Task 11.1: Frontend Shared Infrastructure

**Files:**
- Create: `dts-stack/source/dts-ui/` — shared component library
  - Design tokens (colors, spacing, typography)
  - shadcn/ui components customized for DTS
  - gRPC-Web client wrappers
  - Auth hooks (Keycloak OIDC)
  - SSE event stream hooks
  - Common layouts

**Steps:**

**Step 1:** Initialize shared UI library with Vite library mode

**Step 2:** Implement auth hooks, gRPC-Web client, SSE hooks

**Step 3:** Commit

```bash
git commit -m "feat(s11): create dts-ui shared component library"
```

---

### Task 11.2: dts-admin-webapp — System Admin Portal

**Files:**
- Create: `dts-stack/source/dts-admin-webapp/` (rewrite)
- Key pages:
  - User management (CRUD, role assignment)
  - Tenant management
  - System configuration
  - Audit log viewer
  - Service health dashboard
  - AppPack management

**Steps:** Implement page by page, test, commit.

```bash
git commit -m "feat(s11): implement dts-admin-webapp — system admin portal"
```

---

### Task 11.3: dts-cortex-webapp — Technical Analyst Portal

**Files:**
- Create: `dts-stack/source/dts-cortex-webapp/` (rewrite)
- Key pages:
  - Ontology designer (React Flow graph editor)
  - Data source management
  - Query workbench (SQL + NL2SQL)
  - Data governance (lineage viewer, classification)
  - Data quality dashboard
  - Skill management
  - KnowHow management

**Steps:** Implement page by page, test, commit.

```bash
git commit -m "feat(s11): implement dts-cortex-webapp — technical analyst portal"
```

---

### Task 11.4: dts-pilot-webapp — Decision Maker Portal

**Files:**
- Create: `dts-stack/source/dts-pilot-webapp/` (rewrite)
- Key pages:
  - AI conversation (Vercel AI SDK streaming)
  - Dashboard builder (@dnd-kit drag-and-drop)
  - Report viewer
  - Approval inbox (HITL)
  - Intent history & execution trace viewer

**Steps:** Implement page by page, test, commit.

```bash
git commit -m "feat(s11): implement dts-pilot-webapp — decision maker portal"
```

---

## Sprint 12: Integration & Release (Week 23-24)

### Task 12.1: E2E Integration Tests

**Files:**
- Create: `dts-stack/tests/e2e/`
- Key test scenarios:
  1. User login → JWT → query data → data security masking
  2. NL intent → parse → plan → execute skills → audit trail
  3. High-risk action → HITL approval → execute → audit
  4. Ontology CRUD → lineage tracking → impact analysis
  5. AppPack install → skill register → invoke

**Steps:** Implement all 5 critical paths, verify end-to-end.

```bash
git commit -m "test(s12): add E2E integration tests for 5 critical paths"
```

---

### Task 12.2: Helm Charts

**Files:**
- Create: `dts-stack/deploy/helm/dts/`
  - `Chart.yaml`
  - `values.yaml`
  - `values-offline.yaml`
  - Templates for all 25 services
  - Middleware sub-charts (PG, Kafka, ClickHouse, Neo4j, MinIO, Keycloak)

**Steps:**

**Step 1:** Create umbrella Helm chart with all components

**Step 2:** Parameterize for online/offline (only LLM endpoint + registry differ)

**Step 3:** Test with `helm template` and `helm install --dry-run`

**Step 4:** Commit

```bash
git commit -m "feat(s12): add Helm charts for complete DTS deployment"
```

---

### Task 12.3: Offline Bundle

**Files:**
- Modify: `dts-stack/infra/internal/cli/bundle.go`
- Create: `dts-stack/deploy/offline/`

**Steps:**

**Step 1:** Implement `dts bundle create` — packages all images + charts + config

**Step 2:** Implement `dts bundle install` — loads images, applies charts

**Step 3:** Test offline installation on clean machine

**Step 4:** Commit

```bash
git commit -m "feat(s12): implement offline bundle for air-gapped deployment"
```

---

### Task 12.4: Release v3.0.0

**Steps:**

**Step 1:** Version bump to 3.0.0

**Step 2:** Generate CHANGELOG.md

**Step 3:** Tag and create GitHub Release

**Step 4:** Build and push all Docker images

**Step 5:** Create offline bundle

```bash
git tag -a v3.0.0 -m "DTS AI Decision OS v3.0.0"
git push origin v3.0.0
```

---

## Task Summary by Sprint

| Sprint | Tasks | Est. Story Points |
|--------|-------|-------------------|
| S1: Foundation | 6 tasks (monorepo × 3, proto, docker-compose, CI) | 21 |
| S2: Core Platform | 2 tasks (gateway, platform) | 16 |
| S3: Data Layer | 2 tasks (ontology-store, query-service) | 16 |
| S4: Security & Audit | 3 tasks (data-security, audit-log, workflow) | 21 |
| S5: Governance & Assets | 3 tasks (governance, asset, data-service) | 18 |
| S6: AI Core 1 | 2 tasks (intent-engine, agent) | 21 |
| S7: AI Core 2 | 2 tasks (ontology-engine, query-ai) | 16 |
| S8: AI Data | 3 tasks (data-connector, data-quality, scheduler) | 21 |
| S9: AI Ops | 2 tasks (ai-eval, observability) | 13 |
| S10: Infrastructure | 2 tasks (operator, cli) | 18 |
| S11: Frontend | 4 tasks (ui-lib, admin, cortex, pilot) | 26 |
| S12: Integration | 4 tasks (e2e, helm, offline, release) | 21 |
| **Total** | **35 tasks** | **228 points** |

---

## Dependencies Graph

```
S1 (Foundation) ─┬──→ S2 (Platform) ──→ S3 (Data) ──→ S4 (Security) ──→ S5 (Governance)
                  │                                                           │
                  │                                                           ↓
                  ├──→ S6 (AI Core 1) ──→ S7 (AI Core 2) ──→ S8 (AI Data) → S9 (AI Ops)
                  │                                                           │
                  ├──→ S10 (Infrastructure) ──────────────────────────────────┤
                  │                                                           ↓
                  └──→ S11 (Frontend) ← depends on S2-S9 APIs ──→ S12 (Integration)
```

**Note:** S6-S9 (Python) can partially overlap with S3-S5 (Java) if team has parallel capacity. S10 (Go) can start after S1 is done. Frontend (S11) depends on backend APIs being available.
