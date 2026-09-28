# PRS Screen Logical Datasource Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let PRS screen assets bind to a stable logical datasource alias so local, multi-warehouse, and federated-query environments can route the same screen JSON without embedding environment-specific database ids.

**Architecture:** Screen component SQL configs may carry a string datasource reference such as `prs.flowerbiz.federated`. The webapp passes that reference through to `/api/dataset`, and analytics resolves it to an `analytics_database.id` from configured aliases in `analytics_database.details_json`, preferring business-primary databases over secondary dbt marts when multiple databases advertise the same alias. Liquibase seeds the PRS alias for the current dbt mart and migrates existing PRS screen JSON away from invalid `{8}` placeholders.

**Tech Stack:** Spring Boot 3, JPA repositories, Jackson, Liquibase XML/SQL, React 19, TypeScript, Vitest, JUnit 5, Mockito.

---

### Task 1: Backend Alias Resolution

**Files:**
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/AnalyticsDatabaseAliasResolver.java`
- Test: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/AnalyticsDatabaseAliasResolverTest.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/DatasetResource.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/ScreenWarmupService.java`

- [x] **Step 1: Write failing resolver tests**

Cover numeric passthrough, `details_json.logicalSourceAliases`, duplicate alias preference for `BUSINESS_PRIMARY`, and missing alias errors.

- [x] **Step 2: Run resolver tests and confirm they fail**

Run: `mvn -pl dts-copilot-analytics -Dtest=AnalyticsDatabaseAliasResolverTest test`
Expected: compilation/test failure because the resolver class does not exist.

- [x] **Step 3: Implement the resolver and wire dataset/warmup callers**

Resolve `database` JSON values as numeric id or configured alias. Keep old numeric behavior unchanged.

- [x] **Step 4: Re-run resolver tests**

Run: `mvn -pl dts-copilot-analytics -Dtest=AnalyticsDatabaseAliasResolverTest test`
Expected: tests pass.

### Task 2: Webapp Pass-Through For Logical References

**Files:**
- Modify: `dts-copilot-webapp/src/pages/screens/types.ts`
- Modify: `dts-copilot-webapp/src/pages/screens/hooks/useCardDataSource.ts`
- Test: `dts-copilot-webapp/src/pages/screens/hooks/useCardDataSource.test.ts`

- [x] **Step 1: Write failing hook/helper tests**

Cover SQL data sources with `databaseId: "prs.flowerbiz.federated"` and `databaseAlias: "prs.flowerbiz.federated"` producing `/api/dataset` payloads with `database` set to the alias instead of failing client-side.

- [x] **Step 2: Run the focused Vitest file and confirm failure**

Run: `pnpm --dir dts-copilot-webapp test src/pages/screens/hooks/useCardDataSource.test.ts`
Expected: failure because string datasource references are currently rejected.

- [x] **Step 3: Update types and runtime parsing**

Allow SQL configs to carry `databaseId?: number | string` and `databaseAlias?: string`; use the resolved reference in cache keys and dataset query payloads.

- [x] **Step 4: Re-run focused Vitest**

Run: `pnpm --dir dts-copilot-webapp test src/pages/screens/hooks/useCardDataSource.test.ts`
Expected: tests pass.

### Task 3: Liquibase Seed And Existing Data Repair

**Files:**
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0064_prs_flowerbiz_copilot_screen_records.xml`
- Create: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0065_prs_flowerbiz_screen_logical_datasource.xml`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml`
- Test: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/PrsFlowerbizScreenLogicalDatasourceSeedTest.java`

- [x] **Step 1: Write failing seed verification tests**

Assert 0064/0065 use `prs.flowerbiz.federated`, no longer create `"{8}"` style `databaseId`, and include the new changelog in `master.xml`.

- [x] **Step 2: Run seed test and confirm failure**

Run: `mvn -pl dts-copilot-analytics -Dtest=PrsFlowerbizScreenLogicalDatasourceSeedTest test`
Expected: failure before changelog edits.

- [x] **Step 3: Update seed/migration**

Seed `analytics_database.details_json.logicalSourceAliases` for the dbt mart and migrate PRS screen component JSON to the logical alias.

- [x] **Step 4: Re-run seed test**

Run: `mvn -pl dts-copilot-analytics -Dtest=PrsFlowerbizScreenLogicalDatasourceSeedTest test`
Expected: tests pass.

### Task 4: Runtime Verification

**Files:**
- No new production files unless tests expose a gap.

- [x] **Step 1: Run focused backend and frontend tests**

Run:
`mvn -pl dts-copilot-analytics -Dtest=AnalyticsDatabaseAliasResolverTest,PrsFlowerbizScreenLogicalDatasourceSeedTest test`
`pnpm --dir dts-copilot-webapp test src/pages/screens/hooks/useCardDataSource.test.ts src/pages/screens/specV2.test.ts`

- [x] **Step 2: Rebuild/restart if code reaches green**

Run `mvn clean package -DskipTests`, rebuild affected images, restart the Compose app services, and verify `/api/screens/290006?mode=draft` plus component database refs in `analytics_screen`.

- [x] **Step 3: Record evidence in Sprint-29 worklog**

Update Sprint-29 F6 evidence with the logical datasource alias, current binding, and verification commands.
