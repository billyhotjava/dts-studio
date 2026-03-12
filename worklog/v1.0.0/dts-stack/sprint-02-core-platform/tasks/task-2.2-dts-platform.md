# Task 2.2: dts-platform — User/Tenant/Config/RBAC

**Sprint:** 2 — Core Platform
**Points:** 8
**Status:** TODO

## Files

- Create: `dts-stack/source/dts-platform/` (new Spring Boot 3 project)
- Key classes:
  - `PlatformGrpcService.java` — implements PlatformService proto
  - `UserService.java` — User CRUD + Keycloak sync
  - `TenantService.java` — Tenant management
  - `ConfigService.java` — System configuration
  - `RbacService.java` — Role-based access control
- Migration: `V1__init_platform.sql`
- Tests: unit + integration (Testcontainers PG + Keycloak)

## DB Schema

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

## Steps

1. Create project skeleton with Spring Boot 3 + gRPC + Flyway
2. Write Flyway migration V1__init_platform.sql
3. Write UserService unit tests
4. Implement UserService
5. Write TenantService tests
6. Implement TenantService
7. Write RbacService tests
8. Implement RbacService
9. Implement PlatformGrpcService (glue layer)
10. Write integration tests with Testcontainers
11. Run `mvn clean verify`
12. Commit

```bash
git commit -m "feat(s2): implement dts-platform with user/tenant/rbac/config"
```
