# Copilot Dynamic Datasource Binding Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make Copilot schema lookup and query execution run against the selected external datasource instead of the application primary datasource.

**Architecture:** `analytics` resolves UI-selected `analytics_database.id` to the linked AI datasource ID before proxying chat requests. `copilot-ai` introduces a dedicated datasource registry for tools, and `schema_lookup` / `execute_query` consume that registry instead of the primary Spring datasource.

**Tech Stack:** Spring Boot 3.4, JPA, RestClient, HikariCP, JUnit 5, Mockito, H2.

---

### Task 1: Document the sprint work item

**Files:**
- Modify: `worklog/v1.0.0/sprint-9/README.md`
- Create: `worklog/v1.0.0/sprint-9/tasks/CS-11-copilot-dynamic-datasource-binding.md`

**Step 1: Write the work item**

Describe the bug, scope, dependencies, and acceptance criteria for dynamic datasource binding.

**Step 2: Verify the sprint index references the task**

Confirm the README contains the new task row and dependency note.

### Task 2: Add failing analytics mapping tests

**Files:**
- Create: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/CopilotChatDataSourceResolverTest.java`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotChatDataSourceResolver.java`

**Step 1: Write the failing test**

Cover:
- analytics database ID resolves to linked `dataSourceId`
- missing linked `dataSourceId` on an existing analytics database raises an error
- raw numeric ID passes through when no analytics database exists

**Step 2: Run the test to verify RED**

Run: `mvn -pl dts-copilot-analytics -Dtest=CopilotChatDataSourceResolverTest test`

Expected: test fails because the resolver does not exist yet.

**Step 3: Implement the minimal resolver**

Use `AnalyticsDatabaseRepository` and `ObjectMapper` to read `details_json`.

**Step 4: Run the test to verify GREEN**

Run: `mvn -pl dts-copilot-analytics -Dtest=CopilotChatDataSourceResolverTest test`

Expected: PASS.

### Task 3: Wire analytics chat proxy to the resolver

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotChatResource.java`

**Step 1: Add the failing behavior test or manual assertion point**

At minimum, make the new resolver-driven branch compile-fail until the resource is wired.

**Step 2: Implement minimal wiring**

- inject `CopilotChatDataSourceResolver`
- translate selected database IDs before proxying chat send requests
- return `400` when the selected database has no linked AI datasource

**Step 3: Re-run analytics tests**

Run: `mvn -pl dts-copilot-analytics -Dtest=CopilotChatDataSourceResolverTest,CopilotAiClientTest test`

Expected: PASS.

### Task 4: Add failing AI tool datasource tests

**Files:**
- Create: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/tool/builtin/SchemaLookupToolTest.java`
- Create: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/tool/builtin/ExecuteQueryToolTest.java`
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/ToolConnectionProvider.java`

**Step 1: Write the failing tests**

Cover:
- `schema_lookup` fails without a selected external datasource
- `schema_lookup` lists tables from an external datasource
- `execute_query` returns query rows from an external datasource

Use H2 test datasources and a stub `ToolConnectionProvider`.

**Step 2: Run the tests to verify RED**

Run: `mvn -pl dts-copilot-ai -Dtest=SchemaLookupToolTest,ExecuteQueryToolTest test`

Expected: FAIL because tools still depend on the primary datasource.

### Task 5: Implement AI datasource registry and wire the tools

**Files:**
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/ManagedToolConnectionProvider.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/builtin/SchemaLookupTool.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/builtin/ExecuteQueryTool.java`
- Modify: `dts-copilot-ai/pom.xml`

**Step 1: Implement the provider**

- load `AiDataSource` by `ToolContext.dataSourceId`
- build cached Hikari datasources keyed by datasource configuration
- return JDBC connections for tool execution
- fail clearly when no datasource is selected

**Step 2: Switch tools to the provider**

Remove direct dependence on the primary Spring datasource for generic tools.

**Step 3: Add MySQL runtime driver**

Add `mysql-connector-j` to `dts-copilot-ai`.

**Step 4: Run the tests to verify GREEN**

Run: `mvn -pl dts-copilot-ai -Dtest=SchemaLookupToolTest,ExecuteQueryToolTest,AiDataSourceServiceTest test`

Expected: PASS.

### Task 6: Verify end-to-end behavior

**Files:**
- No code changes required unless smoke uncovers a defect

**Step 1: Compile affected modules**

Run: `mvn -pl dts-copilot-ai,dts-copilot-analytics -DskipTests compile`

**Step 2: Smoke the live datasource path**

Use the provided `rs_cloud_flower` MySQL datasource to confirm:
- datasource can connect
- Copilot-selected database maps to the correct AI datasource
- schema lookup returns business tables instead of the application DB

**Step 3: Record outcome in the sprint task**

Update the task status and verification notes.
