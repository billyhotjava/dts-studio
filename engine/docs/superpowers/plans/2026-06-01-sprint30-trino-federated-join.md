# Sprint-30 F6 Trino Federated Join Plan

## Goal

Restore `dts-trino` in the local dts-stack and make `dts-copilot` use it as the real federated-query entry for PRS flowerbiz cross-database joins.

## Scope

- Add `dts-trino` back to `/opt/prod/s10/v2.2.3/docker-compose-app.yml`.
- Add Trino PostgreSQL and MySQL catalogs without literal passwords.
- Register a `trino` analytics database named `联邦查询入口`.
- Route `prs.flowerbiz.federated` to the Trino gateway as `BUSINESS_PRIMARY`.
- Add query guardrails for Trino federated native SQL.
- Add Sprint-30 F6 worklog and IT evidence.

## Success Criteria

- `docker compose config` renders a `dts-trino` service.
- Trino catalogs include `postgres` and `mysql`.
- A live query can join `postgres.public.*` and `mysql.rs_cloud_flower.*` through Trino.
- `dts-copilot-analytics` has the Trino JDBC driver on the runtime classpath.
- Native federated SQL must use `catalog.schema.table` and cross-catalog joins must include `WHERE` or `LIMIT`.

## Tasks

1. Add RED tests for the Trino driver, federated seed, and SQL guardrail.
2. Add `dts-trino` Compose service and catalog files.
3. Add analytics Trino dependency, seed changelog, and guardrail service.
4. Wire guardrail metadata into `/api/dataset` responses.
5. Add F6 IT script and evidence summary.
6. Rebuild/restart `dts-trino` and `dts-copilot-analytics`.
7. Review code/design and run static/runtime checks.
