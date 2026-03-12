# Task 2.1: dts-gateway — API Gateway

**Sprint:** 2 — Core Platform
**Points:** 8
**Status:** TODO

## Files

- Create: `dts-stack/source/dts-gateway/pom.xml`
- Create: `src/main/java/com/dts/gateway/GatewayApplication.java`
- Create: `src/main/java/com/dts/gateway/config/SecurityConfig.java`
- Create: `src/main/java/com/dts/gateway/config/RouteConfig.java`
- Create: `src/main/java/com/dts/gateway/filter/JwtAuthFilter.java`
- Create: `src/main/java/com/dts/gateway/filter/RateLimitFilter.java`
- Create: `src/main/java/com/dts/gateway/filter/AuditFilter.java`
- Create: `src/main/java/com/dts/gateway/filter/GrpcWebFilter.java`
- Create: `src/main/resources/application.yml`
- Create: `Dockerfile`
- Test: `src/test/java/com/dts/gateway/filter/JwtAuthFilterTest.java`
- Test: `src/test/java/com/dts/gateway/filter/RateLimitFilterTest.java`

## Key Design

- Spring Cloud Gateway reactive
- JWT validation against Keycloak JWKS endpoint (no session state)
- Route to all backend services via config
- Rate limit per tenant (Redis-backed or in-memory)
- gRPC-Web → gRPC translation for frontend
- AuditFilter publishes ALL requests to Kafka `dts.audit.events`
- **Iron Law 2**: This is the ONLY entry point for external requests

## Steps

1. Write JwtAuthFilter tests (valid/invalid/expired JWT)
2. Run tests — verify they fail
3. Implement JwtAuthFilter
4. Run tests — verify pass
5. Write RateLimitFilter tests
6. Implement RateLimitFilter
7. Implement AuditFilter (Kafka publish)
8. Implement GrpcWebFilter
9. Create RouteConfig for all backend services
10. Create Dockerfile (multi-stage, distroless base)
11. Run `mvn clean verify`
12. Commit

```bash
git commit -m "feat(s2): implement dts-gateway with JWT auth, rate limiting, audit"
```
