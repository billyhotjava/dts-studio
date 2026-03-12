# Sprint 2: Core Platform (Java)

**Weeks:** 3-4
**Theme:** API Gateway + User/Tenant/RBAC platform
**Story Points:** 16

## Goals
- Implement dts-gateway (JWT auth, rate limiting, routing, gRPC-Web)
- Implement dts-platform (users, tenants, roles, permissions, config, Keycloak sync)

## Tasks

| # | Task | Points | Status |
|---|------|--------|--------|
| 2.1 | dts-gateway — API Gateway | 8 | TODO |
| 2.2 | dts-platform — User/Tenant/Config/RBAC | 8 | TODO |

## Dependencies
- S1: Parent POM, dts-common, proto definitions, docker-compose

## Deliverables
- [ ] Gateway: JWT validation, route config, rate limiting, audit filter, gRPC-Web
- [ ] Platform: User CRUD, Tenant CRUD, RBAC, Config, Keycloak sync
- [ ] Flyway migrations applied
- [ ] Integration tests with Testcontainers passing
- [ ] Dockerfiles built
