# Fixed Report Business DB Resolution Design

## Problem

Production fixed reports fail with `FIXED_REPORT_EXECUTION_FAILED` because report templates persist `queryContract.databaseName = "园林业务库"`, and the runtime resolves databases by display name. In production, that name points to a system PostgreSQL runtime database instead of the MySQL business database.

The Data page also exposes Copilot runtime PostgreSQL databases, which should not be selectable as business data sources.

## Root Cause

1. Fixed report execution in `DefaultFixedReportExecutionService` resolves authority SQL reports by `databaseName` string only.
2. Several promoted report templates hardcode `databaseName = "园林业务库"`.
3. Database listing in `DatabaseResource` returns all `analytics_database` rows, including runtime/system PostgreSQL databases.

## Chosen Fix

1. For authority-style fixed reports, prefer a business database over runtime/system databases.
2. Treat Copilot runtime PostgreSQL databases as internal and hide them from `/api/database`.
3. Add a forward-only Liquibase change to normalize existing fixed report templates so they no longer hardcode the runtime database name.

## Verification

1. Unit test: authority fixed report falls back from a stale system database name to a business MySQL database.
2. Unit test: `/api/database` excludes runtime PostgreSQL databases.
3. Local analytics tests pass.
4. Production deploy and smoke test:
   - `/api/database` only shows business databases.
   - representative fixed reports run successfully on `ai.yuzhicloud.com`.
