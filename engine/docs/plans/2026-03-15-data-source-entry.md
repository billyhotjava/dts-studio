# Data Source Entry Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add manual datasource creation to the `Data` module while preserving the existing import flow.

**Architecture:** `dts-copilot-ai` becomes the canonical datasource owner. `dts-copilot-analytics` imports AI-managed datasources into `analytics_database` and continues metadata sync. `dts-copilot-webapp` exposes a dual-mode add-datasource page.

**Tech Stack:** Spring Boot 3.4, Spring MVC, Spring Data JPA, Liquibase, React 19, TypeScript, React Router 7, Node built-in test runner.

---

### Task 1: Add AI datasource persistence and CRUD contract

**Files:**
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/AiDataSource.java`
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/repository/AiDataSourceRepository.java`
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/datasource/AiDataSourceService.java`
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/DataSourceResource.java`
- Create: `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_007__ai_data_source.xml`
- Modify: `dts-copilot-ai/src/main/resources/config/liquibase/master.xml`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/datasource/AiDataSourceServiceTest.java`

**Step 1: Write the failing test**

- Add tests for create/list/get/update/delete behavior and secret masking.

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai -Dtest=AiDataSourceServiceTest test`
Expected: FAIL because datasource persistence and service do not exist yet.

**Step 3: Write minimal implementation**

- Add datasource entity, repository, service, REST resource, and Liquibase table.

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai -Dtest=AiDataSourceServiceTest test`
Expected: PASS

### Task 2: Add AI datasource connection test

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/datasource/AiDataSourceService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/DataSourceResource.java`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/datasource/AiDataSourceConnectionTest.java`

**Step 1: Write the failing test**

- Add tests for successful validation and failure mapping.

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai -Dtest=AiDataSourceConnectionTest test`
Expected: FAIL because connection test support does not exist yet.

**Step 3: Write minimal implementation**

- Build JDBC config from stored datasource details and return structured test results.

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai -Dtest=AiDataSourceConnectionTest test`
Expected: PASS

### Task 3: Switch analytics import flow from platform-only to datasource-based

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotAiClient.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/PlatformInfraClient.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/JdbcDetailsResolver.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResource.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/PlatformIntegrationResource.java`
- Test: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResourceDataSourceTest.java`

**Step 1: Write the failing test**

- Add tests proving `/api/database` accepts `details.dataSourceId`, imports datasource details from AI, and still reads legacy `platformDataSourceId`.

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-analytics -Dtest=DatabaseResourceDataSourceTest test`
Expected: FAIL because create/update/validate currently only allow platform datasource IDs.

**Step 3: Write minimal implementation**

- Make datasource lookup generic, keep legacy aliases for backward compatibility, and import from AI datasource summaries/details.

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-analytics -Dtest=DatabaseResourceDataSourceTest test`
Expected: PASS

### Task 4: Add webapp API helpers for manual datasource flow

**Files:**
- Modify: `dts-copilot-webapp/src/api/analyticsApi.ts`
- Test: `dts-copilot-webapp/tests/dataSourceEntryApi.test.ts`

**Step 1: Write the failing test**

- Add tests for:
  - create AI datasource payload shaping
  - import analytics database payload shaping
  - create-and-import orchestration helpers

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/dataSourceEntryApi.test.ts --bundle --platform=node --format=esm --outfile=/tmp/dataSourceEntryApi.test.mjs && node --test /tmp/dataSourceEntryApi.test.mjs`
Expected: FAIL because the helpers do not exist yet.

**Step 3: Write minimal implementation**

- Add AI datasource CRUD/test helpers and manual create/import helper functions.

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/dataSourceEntryApi.test.ts --bundle --platform=node --format=esm --outfile=/tmp/dataSourceEntryApi.test.mjs && node --test /tmp/dataSourceEntryApi.test.mjs`
Expected: PASS

### Task 5: Convert `/data/new` into dual-mode add page

**Files:**
- Modify: `dts-copilot-webapp/src/pages/DatabaseNewPage.tsx`
- Modify: `dts-copilot-webapp/src/i18n.ts`
- Test: `dts-copilot-webapp/tests/databaseNewPage.test.ts`

**Step 1: Write the failing test**

- Add tests for mode switching, manual form validation, and the create -> import -> sync flow.

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/databaseNewPage.test.ts --bundle --platform=node --format=esm --outfile=/tmp/databaseNewPage.test.mjs && node --test /tmp/databaseNewPage.test.mjs`
Expected: FAIL because the page only supports import-existing today.

**Step 3: Write minimal implementation**

- Add mode tabs, manual form, test-connection action, and create/import/sync orchestration.

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/databaseNewPage.test.ts --bundle --platform=node --format=esm --outfile=/tmp/databaseNewPage.test.mjs && node --test /tmp/databaseNewPage.test.mjs`
Expected: PASS

### Task 6: Run focused verification

**Files:**
- Modify: `docs/plans/2026-03-15-data-source-entry-design.md`
- Modify: `docs/plans/2026-03-15-data-source-entry.md`

**Step 1: Run backend verification**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai,dts-copilot-analytics -DskipTests compile`
Expected: PASS

**Step 2: Run focused backend tests**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai -Dtest=AiDataSourceServiceTest,AiDataSourceConnectionTest test && mvn -pl dts-copilot-analytics -Dtest=DatabaseResourceDataSourceTest test`
Expected: PASS

**Step 3: Run frontend verification**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && pnpm run build:modern`
Expected: PASS

**Step 4: Run focused frontend tests**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/dataSourceEntryApi.test.ts --bundle --platform=node --format=esm --outfile=/tmp/dataSourceEntryApi.test.mjs && node --test /tmp/dataSourceEntryApi.test.mjs && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/databaseNewPage.test.ts --bundle --platform=node --format=esm --outfile=/tmp/databaseNewPage.test.mjs && node --test /tmp/databaseNewPage.test.mjs`
Expected: PASS
