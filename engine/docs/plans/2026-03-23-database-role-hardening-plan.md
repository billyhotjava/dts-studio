# Database Role Hardening Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add explicit `database_role` modeling so fixed reports, database visibility, and default registration no longer depend on environment-specific database names.

**Architecture:** Introduce `database_role` on `analytics_database`, resolve fixed reports by role instead of `databaseName`, and add a forward-only Liquibase migration that cleans historical template contracts and backfills roles.

**Tech Stack:** Spring Boot 3.4, JPA, Liquibase, JUnit 5, Mockito

---

### Task 1: Add database role model and failing tests

**Files:**
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsDatabaseRole.java`
- Modify: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionServiceTest.java`
- Modify: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/DefaultDatabaseInitServiceTest.java`
- Modify: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResourceTest.java`

**Step 1: Write the failing tests**

- Add a fixed report test proving `BUSINESS_PRIMARY` beats a legacy `databaseName`.
- Add a default database init test proving a configured business entry is persisted with `BUSINESS_PRIMARY`.
- Add a database list test proving `SYSTEM_RUNTIME` is hidden even if the name is not `园林业务库`.

**Step 2: Run tests to verify they fail**

Run:
```bash
mvn -pl dts-copilot-analytics -Dtest=DefaultFixedReportExecutionServiceTest,DefaultDatabaseInitServiceTest,DatabaseResourceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Step 3: Write minimal implementation**

- Add `AnalyticsDatabaseRole`
- Add `databaseRole` field to `AnalyticsDatabase`
- Add minimal getters/setters and enum parsing helpers if needed by tests

**Step 4: Run tests to verify progress**

Run the same Maven command and keep only the new failures.

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsDatabase.java dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsDatabaseRole.java dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionServiceTest.java dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/DefaultDatabaseInitServiceTest.java dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResourceTest.java
git commit -m "引入数据库角色模型"
```

### Task 2: Add Liquibase backfill and template cleanup

**Files:**
- Create: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0055_add_database_role_and_backfill.xml`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml`
- Modify: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/ReportTemplateBackingPromotionVerificationTest.java`

**Step 1: Write the failing verification test**

- Assert the new changelog is included in `master.xml`
- Assert it removes `queryContract.databaseName`
- Assert it writes `queryContract.databaseRole = "BUSINESS_PRIMARY"` for fixed report templates

**Step 2: Run test to verify it fails**

Run:
```bash
mvn -pl dts-copilot-analytics -Dtest=ReportTemplateBackingPromotionVerificationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Step 3: Write minimal Liquibase implementation**

- Add `database_role` column
- Backfill existing `analytics_database`
- Normalize fixed report `spec_json`
- Include the changelog in `master.xml`

**Step 4: Run test to verify it passes**

Run the same Maven command.

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/resources/config/liquibase/master.xml dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0055_add_database_role_and_backfill.xml dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/ReportTemplateBackingPromotionVerificationTest.java
git commit -m "补齐数据库角色迁移"
```

### Task 3: Resolve fixed reports by role

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionService.java`
- Modify: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionServiceTest.java`

**Step 1: Write the failing tests**

- Add a test proving fixed reports prefer `BUSINESS_PRIMARY`
- Add a test proving a runtime database with matching old name is ignored
- Add a test proving legacy `databaseName` only acts as fallback when no role-matching business DB exists

**Step 2: Run test to verify it fails**

Run:
```bash
mvn -pl dts-copilot-analytics -Dtest=DefaultFixedReportExecutionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Step 3: Write minimal implementation**

- Remove the hard dependency on `DEFAULT_DATABASE_NAME`
- Resolve database by `queryContract.databaseRole`
- Keep a narrow compatibility fallback for legacy templates

**Step 4: Run test to verify it passes**

Run the same Maven command.

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionService.java dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionServiceTest.java
git commit -m "按数据库角色解析固定报表"
```

### Task 4: Use roles in default registration and data visibility

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/DefaultDatabaseInitService.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResource.java`
- Modify: `dts-copilot-analytics/src/main/resources/application.yml`
- Modify: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/DefaultDatabaseInitServiceTest.java`
- Modify: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResourceTest.java`

**Step 1: Write the failing tests**

- Default init should persist the configured role
- Runtime databases should be hidden by role
- Business databases should remain listed regardless of name

**Step 2: Run tests to verify they fail**

Run:
```bash
mvn -pl dts-copilot-analytics -Dtest=DefaultDatabaseInitServiceTest,DatabaseResourceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Step 3: Write minimal implementation**

- Extend default database config entries with role
- Replace heuristic visibility with explicit role checks
- Preserve backwards compatibility for old records with null role during the migration window

**Step 4: Run tests to verify they pass**

Run the same Maven command.

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/DefaultDatabaseInitService.java dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResource.java dts-copilot-analytics/src/main/resources/application.yml dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/DefaultDatabaseInitServiceTest.java dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResourceTest.java
git commit -m "按角色收口数据源注册与展示"
```

### Task 5: Run full analytics regression

**Files:**
- No new files expected unless fixes are needed

**Step 1: Run targeted analytics regression**

```bash
mvn -pl dts-copilot-analytics -Dtest=DefaultFixedReportExecutionServiceTest,FixedReportResourceTest,ReportTemplateSeedVerificationTest,ReportTemplateBackingPromotionVerificationTest,DefaultDatabaseInitServiceTest,DatabaseResourceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Step 2: Run compile**

```bash
mvn -pl dts-copilot-analytics -DskipTests compile
```

**Step 3: Commit any final fixes**

```bash
git add .
git commit -m "完成数据库角色收口"
```
