# Sprint 23 Agent BI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Sprint-23 Agent BI so PRS users can turn natural language into fixed reports, report drafts, business details, business insights, and safe action proposals.

**Architecture:** Add a code-backed PRS report catalog in `dts-copilot-ai`, feed it into the planner, and expose report metadata through chat messages/SSE. Persist data surface and quality metadata in AI chat messages and analytics drafts. Render the metadata in the Copilot report card and add a reproducible PRS Golden Questions smoke script.

**Tech Stack:** Spring Boot, JPA, Liquibase, JUnit 5/Mockito/AssertJ, React, TypeScript, Vitest.

---

### Task 1: Backend Agent BI Report Catalog

**Files:**
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AgentBiReportCatalogService.java`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/AgentBiReportCatalogServiceTest.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ConversationPlannerService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AssetBackedPlannerPolicy.java`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/AssetBackedPlannerPolicyTest.java`

- [ ] Write tests for report catalog domain coverage, L1/L2/L0 matching, and safe action proposal matching.
- [ ] Run backend tests and confirm failures are for missing catalog/metadata behavior.
- [ ] Implement catalog entries and planner metadata fields.
- [ ] Run backend tests and confirm pass.

### Task 2: Chat Message and Draft Metadata

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/AiChatMessage.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AgentChatResource.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/InternalAgentChatResource.java`
- Create: `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_019__chat_message_report_metadata.xml`
- Modify: `dts-copilot-ai/src/main/resources/config/liquibase/master.xml`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsAnalysisDraft.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/AnalysisDraftService.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/AnalysisDraftResource.java`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0043_analysis_drafts.xml`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionServiceTest.java`
- Test: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/AnalysisDraftSchemaVerificationTest.java`
- Test: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsAnalysisDraftMappingTest.java`

- [ ] Write failing tests for SSE `dataSurface`, `qualityLevel`, `qualityNotes`, and `reportCode`.
- [ ] Write failing analytics draft tests for quality fields.
- [ ] Implement entity, API, Liquibase, and service propagation.
- [ ] Run targeted backend tests.

### Task 3: Frontend Report Card Metadata and Actions

**Files:**
- Modify: `dts-copilot-webapp/src/api/types.ts`
- Modify: `dts-copilot-webapp/src/api/modules/copilot.ts`
- Modify: `dts-copilot-webapp/src/api/aiChatCompatibility.ts`
- Modify: `dts-copilot-webapp/src/components/copilot/copilotAnalysisDraft.ts`
- Modify: `dts-copilot-webapp/src/components/copilot/copilotGeneratedReportMessage.ts`
- Modify: `dts-copilot-webapp/src/components/copilot/copilotGeneratedReportMessage.test.ts`
- Modify: `dts-copilot-webapp/src/components/copilot/CopilotChat.tsx`
- Modify: `dts-copilot-webapp/src/components/copilot/InlineSqlPreview.tsx`

- [ ] Write failing Vitest expectations for quality/data-surface report notice and draft payload propagation.
- [ ] Implement type parsing, report-card copy, and draft payload fields.
- [ ] Add an explicit “加入大屏” action route from report drafts.
- [ ] Run targeted frontend tests.

### Task 4: PRS Golden Questions and Sprint Status

**Files:**
- Create: `worklog/v1.0.0/sprint-23-202605/it/golden-questions-prs-agent-bi.md`
- Create: `worklog/v1.0.0/sprint-23-202605/it/test_prs_agent_bi_report.sh`
- Modify: `worklog/v1.0.0/sprint-23-202605/README.md`
- Modify: `worklog/v1.0.0/sprint-queue.md`

- [ ] Add 20 Golden Questions with expected response kind, data surface, and quality level.
- [ ] Add a smoke script that validates response metadata and SQL safety.
- [ ] Update sprint status from `READY` to `IN_PROGRESS` or `DONE` based on verification.
- [ ] Run shell syntax and markdown diff checks.
