# Known Report Fast Path And Copilot Exploration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a report-first analytics flow where certified high-frequency reports bypass live NL2SQL, while Copilot remains the fallback for unknown questions and feeds new report candidates back into the template catalog.

**Architecture:** Add a unified `ReportTemplate` asset model in `analytics`, reuse existing dashboard / screen / report-factory consumers, and change Copilot routing to try certified templates before NL2SQL exploration. Use realtime authority views for state/detail reports and lightweight mart/fact or cache for trend/ranking reports.

**Tech Stack:** Spring Boot 3, JPA, Liquibase, React + Vite, existing analytics APIs, existing Copilot routing/templateCode plumbing.

---

### Task 1: Lock the initial business report inventory

**Files:**
- Create: `worklog/v1.0.0/sprint-14/README.md`
- Create: `worklog/v1.0.0/sprint-14/tasks/RF-01-report-inventory.md`
- Create: `worklog/v1.0.0/sprint-14/it/README.md`
- Modify: `worklog/v1.0.0/sprint-queue.md`

**Step 1: Write the failing test**

No code test. Define a review checklist in `RF-01-report-inventory.md` that is considered incomplete until:
- at least 20 candidate reports are listed
- each report has domain, user role, freshness, display type, and owner
- finance and procurement reports are marked as P0

**Step 2: Verify current inventory is missing**

Run: `cd /opt/prod/prs/source/dts-copilot && rg -n "P0|report inventory|templateCode" worklog/v1.0.0/sprint-14 docs/plans`
Expected: no sprint-14 inventory exists yet

**Step 3: Write minimal implementation**

Create sprint-14 scaffolding and inventory checklist.

**Step 4: Verify it exists**

Run: `cd /opt/prod/prs/source/dts-copilot && sed -n '1,220p' worklog/v1.0.0/sprint-14/tasks/RF-01-report-inventory.md`
Expected: checklist and inventory schema are visible

**Step 5: Commit**

```bash
git add worklog/v1.0.0/sprint-14
git add worklog/v1.0.0/sprint-queue.md
git commit -m "新增报表优先化实施计划"
```

### Task 2: Add the unified report template domain model

**Files:**
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml`
- Create: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0039_report_template_registry.xml`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsReportTemplateRegistry.java`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/repository/AnalyticsReportTemplateRegistryRepository.java`
- Create: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsReportTemplateRegistryMappingTest.java`

**Step 1: Write the failing test**

Write a mapping test that asserts the registry exposes:
- `template_code`
- `domain`
- `category`
- `data_source_type`
- `target_object`
- `refresh_policy`
- `permission_policy_json`
- `parameter_schema_json`
- `metric_definition_json`
- `presentation_schema_json`
- `certification_status`

**Step 2: Run test to verify it fails**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=AnalyticsReportTemplateRegistryMappingTest test`
Expected: FAIL because entity and changelog do not exist

**Step 3: Write minimal implementation**

Create Liquibase + entity + repository for the unified template registry.

**Step 4: Run test to verify it passes**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=AnalyticsReportTemplateRegistryMappingTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/resources/config/liquibase/master.xml
git add dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0039_report_template_registry.xml
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsReportTemplateRegistry.java
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/repository/AnalyticsReportTemplateRegistryRepository.java
git add dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsReportTemplateRegistryMappingTest.java
git commit -m "新增统一报表模板注册表"
```

### Task 3: Expose fixed report catalog APIs

**Files:**
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/ReportTemplateCatalogService.java`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/ReportTemplateCatalogResource.java`
- Create: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/ReportTemplateCatalogResourceTest.java`

**Step 1: Write the failing test**

Add controller tests for:
- list all active templates
- filter by `domain`
- filter by `category`
- return only certified templates by default

**Step 2: Run test to verify it fails**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportTemplateCatalogResourceTest test`
Expected: FAIL because resource does not exist

**Step 3: Write minimal implementation**

Add catalog service and REST endpoints under `/api/analytics/report-catalog`.

**Step 4: Run test to verify it passes**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportTemplateCatalogResourceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/ReportTemplateCatalogService.java
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/ReportTemplateCatalogResource.java
git add dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/ReportTemplateCatalogResourceTest.java
git commit -m "新增固定报表目录接口"
```

### Task 4: Seed finance and procurement certified templates

**Files:**
- Create: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0040_seed_finance_procurement_templates.xml`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml`
- Create: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/ReportTemplateSeedVerificationTest.java`

**Step 1: Write the failing test**

Add a test that verifies the presence of at least:
- 8 finance templates
- 8 procurement / warehouse templates
- valid `template_code`, `domain`, `refresh_policy`, `target_object`

**Step 2: Run test to verify it fails**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportTemplateSeedVerificationTest test`
Expected: FAIL because seed does not exist

**Step 3: Write minimal implementation**

Seed template metadata only, not final business SQL for every template. Keep placeholders explicit where ownership review is required.

**Step 4: Run test to verify it passes**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportTemplateSeedVerificationTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0040_seed_finance_procurement_templates.xml
git add dts-copilot-analytics/src/main/resources/config/liquibase/master.xml
git add dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/ReportTemplateSeedVerificationTest.java
git commit -m "预置财务采购固定报表模板"
```

### Task 5: Add finance and procurement authority data adapters

**Files:**
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/AuthorityQueryService.java`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/ReportExecutionPlanService.java`
- Create: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/report/ReportExecutionPlanServiceTest.java`
- Modify: relevant existing view registry / mart wiring files as needed after discovery

**Step 1: Write the failing test**

Add tests that assert:
- finance state/detail templates route to authority views / authority SQL
- procurement / inventory state/detail templates route to authority views / authority SQL
- trend / ranking templates route to mart/fact or cache plans

**Step 2: Run test to verify it fails**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportExecutionPlanServiceTest test`
Expected: FAIL because service does not exist

**Step 3: Write minimal implementation**

Create plan selection logic:
- `VIEW` for realtime authority detail/state
- `MART` / `FACT` for trend/ranking
- explicit refresh policy metadata

**Step 4: Run test to verify it passes**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportExecutionPlanServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report
git add dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/report/ReportExecutionPlanServiceTest.java
git commit -m "新增固定报表执行计划选择器"
```

### Task 6: Build a fixed report execution API

**Files:**
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/FixedReportResource.java`
- Create: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/FixedReportResourceTest.java`

**Step 1: Write the failing test**

Add controller tests for:
- execute by `templateCode`
- parameter validation
- permission rejection
- response includes metadata: freshness, source type, templateCode

**Step 2: Run test to verify it fails**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=FixedReportResourceTest test`
Expected: FAIL because endpoint does not exist

**Step 3: Write minimal implementation**

Expose `/api/analytics/fixed-reports/{templateCode}/run`.

**Step 4: Run test to verify it passes**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=FixedReportResourceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/FixedReportResource.java
git add dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/FixedReportResourceTest.java
git commit -m "新增固定报表执行接口"
```

### Task 7: Add a fixed report center UI

**Files:**
- Create: `dts-copilot-webapp/src/pages/FixedReportsPage.tsx`
- Create: `dts-copilot-webapp/src/pages/fixed-reports/FixedReportRunPage.tsx`
- Modify: `dts-copilot-webapp/src/routes.tsx`
- Modify: `dts-copilot-webapp/src/layouts/AppLayout.tsx`
- Modify: `dts-copilot-webapp/src/api/analyticsApi.ts`
- Create: `dts-copilot-webapp/tests/fixedReportsPage.test.ts`

**Step 1: Write the failing test**

Add UI tests for:
- domain tabs
- list of certified templates
- parameter form rendering
- execute action state

**Step 2: Run test to verify it fails**

Run: `node --experimental-strip-types --test /opt/prod/prs/source/dts-copilot/dts-copilot-webapp/tests/fixedReportsPage.test.ts`
Expected: FAIL because page and APIs do not exist

**Step 3: Write minimal implementation**

Add:
- report center list page
- run page
- navigation entry
- API methods

**Step 4: Run test to verify it passes**

Run: `node --experimental-strip-types --test /opt/prod/prs/source/dts-copilot/dts-copilot-webapp/tests/fixedReportsPage.test.ts`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-webapp/src/pages/FixedReportsPage.tsx
git add dts-copilot-webapp/src/pages/fixed-reports/FixedReportRunPage.tsx
git add dts-copilot-webapp/src/routes.tsx
git add dts-copilot-webapp/src/layouts/AppLayout.tsx
git add dts-copilot-webapp/src/api/analyticsApi.ts
git add dts-copilot-webapp/tests/fixedReportsPage.test.ts
git commit -m "新增固定报表中心页面"
```

### Task 8: Change Copilot routing to template-first

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/TemplateMatcherService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ChatGroundingService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`
- Modify: `dts-copilot-webapp/src/components/copilot/CopilotChat.tsx`
- Create: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/TemplateFirstRoutingTest.java`

**Step 1: Write the failing test**

Add tests that assert:
- high-confidence fixed report questions resolve to template match
- parameterized variants do not fall through to free NL2SQL
- only low-confidence questions continue to exploration mode

**Step 2: Run test to verify it fails**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-ai -Dtest=TemplateFirstRoutingTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

Change Copilot routing order:
1. fixed report template
2. constrained exploration
3. clarification

**Step 4: Run test to verify it passes**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-ai -Dtest=TemplateFirstRoutingTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/TemplateMatcherService.java
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ChatGroundingService.java
git add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java
git add dts-copilot-webapp/src/components/copilot/CopilotChat.tsx
git add dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/TemplateFirstRoutingTest.java
git commit -m "调整Copilot为模板优先路由"
```

### Task 9: Add report candidate capture from exploration results

**Files:**
- Create: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0041_report_candidate_pool.xml`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsReportCandidate.java`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/ReportCandidateService.java`
- Create: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/ReportCandidateServiceTest.java`

**Step 1: Write the failing test**

Add tests that assert a candidate is recorded with:
- question
- routed domain
- generated SQL
- template hit/miss
- user feedback

**Step 2: Run test to verify it fails**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportCandidateServiceTest test`
Expected: FAIL because table and service do not exist

**Step 3: Write minimal implementation**

Create the candidate pool persistence model and service hooks.

**Step 4: Run test to verify it passes**

Run: `mvn -f /opt/prod/prs/source/dts-copilot/pom.xml -pl dts-copilot-analytics -Dtest=ReportCandidateServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0041_report_candidate_pool.xml
git add dts-copilot-analytics/src/main/resources/config/liquibase/master.xml
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsReportCandidate.java
git add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/ReportCandidateService.java
git add dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/ReportCandidateServiceTest.java
git commit -m "新增报表候选池"
```

### Task 10: Add end-to-end verification for the new dual-engine flow

**Files:**
- Create: `worklog/v1.0.0/sprint-14/it/test_fixed_report_fastpath.sh`
- Create: `worklog/v1.0.0/sprint-14/it/test_copilot_template_first.sh`
- Modify: `worklog/v1.0.0/sprint-14/it/README.md`

**Step 1: Write the failing test**

Define smoke checks for:
- finance fixed report returns without AI call
- procurement fixed report returns without AI call
- unknown query falls back to Copilot exploration
- repeated unknown query can be captured as a report candidate

**Step 2: Run test to verify it fails**

Run: `bash /opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-14/it/test_fixed_report_fastpath.sh`
Expected: FAIL because scripts do not exist

**Step 3: Write minimal implementation**

Add executable shell-based smoke tests with environment assumptions clearly documented.

**Step 4: Run test to verify it passes**

Run:
- `bash /opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-14/it/test_fixed_report_fastpath.sh`
- `bash /opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-14/it/test_copilot_template_first.sh`

Expected: PASS or explicit SKIP with documented prerequisites

**Step 5: Commit**

```bash
git add worklog/v1.0.0/sprint-14/it
git commit -m "补齐固定报表与Copilot双引擎验收脚本"
```
