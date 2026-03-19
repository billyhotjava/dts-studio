# Garden NL2SQL Business Grounding Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the business-grounded, dual-channel NL2SQL foundation for `adminapi/adminweb`, starting with the project fulfillment and field operations domains.

**Architecture:** `analytics` becomes the semantic and execution control plane, while `ai` focuses on intent understanding and SQL/query-plan generation. Runtime splits into a realtime direct-query channel and a lightweight ELT-backed semantic mart channel, both driven by the same compiled business semantics.

**Tech Stack:** Spring Boot 3.4, Java 21, React 19 + Vite, PostgreSQL, MySQL, Liquibase, existing `analytics_*` metadata tables, `adminapi/adminweb` domain code as semantic source material.

---

### Task 1: Domain Inventory And Semantic Source Mapping

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-01-domain-inventory-and-source-mapping.md`
- Reference: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/project/**`
- Reference: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/flowerbiz/**`
- Reference: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/tasknew/**`
- Reference: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/pendulum/**`
- Reference: `adminweb/src/api/flower/**`

**Steps:**
1. Inventory the project fulfillment and field operation entry points in `adminapi/adminweb`.
2. Build the source map from business objects to APIs, controllers, and candidate tables.
3. Save the source-of-truth mapping and review gaps before any runtime changes.
4. Commit the inventory artifacts.

### Task 2: Semantic Object / Field / Relation Foundation

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsTable.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsField.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/Nl2SqlSemanticRecallService.java`
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-02-semantic-object-field-relation-foundation.md`

**Steps:**
1. Define the minimal metadata additions needed for business objects, default time fields, and relation hints.
2. Add storage and read models without breaking existing metadata sync.
3. Expose the semantic fields through analytics APIs needed by recall/runtime compilation.
4. Add focused tests for new metadata read/write behavior.
5. Commit the foundation changes.

### Task 3: Project Fulfillment Semantic Pack

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-03-project-fulfillment-semantic-pack.md`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/Nl2SqlSemanticRecallService.java`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/*`

**Steps:**
1. Define the core project fulfillment objects, fields, time dimensions, and allowed joins.
2. Seed synonyms and few-shot examples for project fulfillment questions.
3. Add eval cases for the highest-value fulfillment prompts.
4. Run semantic recall tests against seeded prompts.
5. Commit the semantic pack.

### Task 4: Field Operation Semantic Pack

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-04-field-operation-semantic-pack.md`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/Nl2SqlSemanticRecallService.java`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/*`

**Steps:**
1. Normalize add/cut/change/transfer/recovery/pendulum/task events into a consistent event vocabulary.
2. Seed synonyms, examples, and eval cases for field operation prompts.
3. Verify that recall favors the field operation tables and metrics instead of unrelated project tables.
4. Commit the semantic pack.

### Task 5: Join Contract And Allowed Table Compilation

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-05-join-contract-and-allowed-table-compilation.md`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/*`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`

**Steps:**
1. Define a compiled representation for allowed tables and legal join paths per domain.
2. Implement compiler output consumable by both AI prompt assembly and analytics execution policy.
3. Add tests that deny off-domain table usage and illegal joins.
4. Commit the compiled contract layer.

### Task 6: Metric Definitions And Metric Store Alignment

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-06-metric-definitions-and-metric-store-alignment.md`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsMetric.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/MetricLensResource.java`

**Steps:**
1. Define the v1 metrics for project fulfillment and field operations.
2. Align metric storage shape with NL2SQL runtime consumption.
3. Add metric lens coverage for versioning and diffing the new business metrics.
4. Add tests that protect metric JSON compatibility.
5. Commit the metric alignment.

### Task 7: Intent Routing And Dual-Channel Selection

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-07-intent-routing-and-dual-channel-selection.md`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotChatResource.java`

**Steps:**
1. Introduce an intent classifier that picks domain plus query type.
2. Map query types to the realtime or semantic-mart channel.
3. Add a clarification branch for low-confidence cases.
4. Cover the router with unit tests and representative prompts.
5. Commit the routing layer.

### Task 8: Realtime Channel Context Compilation And Permission Bridge

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-08-realtime-channel-context-and-permission-bridge.md`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/builtin/SchemaLookupTool.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/tool/builtin/ExecuteQueryTool.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/QueryPermissionService.java`

**Steps:**
1. Compile restricted runtime context for direct-query prompts.
2. Bridge analytics permission checks into Copilot-driven query execution.
3. Enforce allowed tables and join contracts before execution.
4. Add regression tests that block unauthorized NL2SQL paths.
5. Commit the realtime safety bridge.

### Task 9: Lightweight ELT Semantic Mart

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-09-lightweight-elt-semantic-mart.md`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/*`
- Create/Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/*`

**Steps:**
1. Design two v1 marts: project fulfillment daily and field operation event fact.
2. Implement incremental sync jobs with explicit watermarks.
3. Add metadata and ownership registration so the marts appear as first-class analytics databases/tables.
4. Add sync smoke tests against fixture data.
5. Commit the ELT-lite foundation.

### Task 10: IT Integration Suite And Acceptance Matrix

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-10-it-integration-and-acceptance-matrix.md`
- Create: `worklog/v1.0.0/sprint-10/it/README.md`
- Create: `worklog/v1.0.0/sprint-10/it/BG-10-dual-channel-nl2sql-matrix.md`

**Steps:**
1. Define the end-to-end scenarios spanning adminweb, copilot, analytics, and the business database.
2. Separate realtime-query acceptance from semantic-mart acceptance.
3. Record required fixtures, credentials, and expected outputs.
4. Add smoke commands and manual verification checklists.
5. Commit the IT suite definition.
