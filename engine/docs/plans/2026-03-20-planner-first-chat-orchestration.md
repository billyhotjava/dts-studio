# Planner-First Chat Orchestration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the current clarification-first chat entry with a planner-first orchestration flow that always lets valid requests reach the agent/tool pipeline.

**Architecture:** Introduce a dedicated conversation planner that classifies chat requests into direct reply, template fast path, or agent workflow. Demote business routing into contextual hints instead of a global gate, and persist a structured `responseKind` so the frontend no longer depends on brittle clarification strings.

**Tech Stack:** Java 21, Spring Boot 3.4, Liquibase, JUnit 5, React 19, TypeScript, node:test

---

### Task 1: Planner Contract

**Files:**
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ConversationPlannerService.java`
- Create: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/ConversationPlannerServiceTest.java`

**Step 1: Write the failing test**

Cover:
- greeting -> `DIRECT_RESPONSE`
- assistant meta -> `DIRECT_RESPONSE`
- metadata exploration -> `AGENT_WORKFLOW`
- ambiguous business question -> `AGENT_WORKFLOW`
- exact template -> `TEMPLATE_FAST_PATH`

**Step 2: Run test to verify it fails**

Run: `mvn -pl dts-copilot-ai -Dtest=ConversationPlannerServiceTest test`

**Step 3: Write minimal implementation**

Introduce:
- `PlanMode`
- `ResponseKind`
- `ConversationPlan`
- `ConversationPlannerService`

Reuse existing router/template/semantic collaborators.

**Step 4: Run test to verify it passes**

Run: `mvn -pl dts-copilot-ai -Dtest=ConversationPlannerServiceTest test`

**Step 5: Commit**

`git commit -m "feat: add planner-first conversation contract"`

### Task 2: Replace Clarification Hard Gate

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`
- Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatServiceTest.java`
- Create or Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionServiceTest.java`

**Step 1: Write the failing test**

Add a test proving that an ambiguous business question and a metadata-exploration question both reach the agent workflow instead of being returned as direct clarification.

**Step 2: Run test to verify it fails**

Run: `mvn -pl dts-copilot-ai -Dtest=AgentExecutionServiceTest,AgentChatServiceTest test`

**Step 3: Write minimal implementation**

Change `AgentExecutionService` to:
- use `ConversationPlan`
- only short-circuit `DIRECT_RESPONSE`
- keep template fast path
- send all `AGENT_WORKFLOW` cases through ReAct

**Step 4: Run test to verify it passes**

Run: `mvn -pl dts-copilot-ai -Dtest=AgentExecutionServiceTest,AgentChatServiceTest test`

**Step 5: Commit**

`git commit -m "refactor: remove clarification hard gate from chat execution"`

### Task 3: Persist Structured Response Kind

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/AiChatMessage.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/InternalAgentChatResource.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AgentChatResource.java`
- Create: `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_014__chat_message_response_kind.xml`

**Step 1: Write the failing test**

Add tests that assert assistant messages expose `responseKind` in session detail payloads.

**Step 2: Run test to verify it fails**

Run: `mvn -pl dts-copilot-ai -Dtest=InternalAgentChatResourceTest test`

**Step 3: Write minimal implementation**

Persist and serialize `responseKind`.

**Step 4: Run test to verify it passes**

Run: `mvn -pl dts-copilot-ai -Dtest=InternalAgentChatResourceTest test`

**Step 5: Commit**

`git commit -m "feat: persist structured chat response kind"`

### Task 4: Frontend Session Restore Decoupling

**Files:**
- Modify: `dts-copilot-webapp/src/api/analyticsApi.ts`
- Modify: `dts-copilot-webapp/src/components/copilot/copilotSessionBootstrap.ts`
- Modify: `dts-copilot-webapp/tests/copilotSessionBootstrap.test.ts`

**Step 1: Write the failing test**

Add a test showing that greeting guidance is identified by `responseKind` even when the legacy clarification prefix is absent.

**Step 2: Run test to verify it fails**

Run: `node --test dts-copilot-webapp/tests/copilotSessionBootstrap.test.ts`

**Step 3: Write minimal implementation**

Add `responseKind` to frontend chat types and prefer it in bootstrap restore logic.

**Step 4: Run test to verify it passes**

Run: `node --test dts-copilot-webapp/tests/copilotSessionBootstrap.test.ts`

**Step 5: Commit**

`git commit -m "refactor: use structured response kinds in session restore"`

### Task 5: Full Regression

**Files:**
- Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/ChatGroundingServiceTest.java`
- Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/IntentRouterServiceTest.java`
- Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatServiceTest.java`
- Modify: `dts-copilot-webapp/tests/copilotSessionBootstrap.test.ts`

**Step 1: Write the failing test**

Add or update regression tests for:
- metadata exploration
- ambiguous business question
- greeting
- template fast path
- legacy session fallback

**Step 2: Run test to verify it fails**

Run:
- `mvn -pl dts-copilot-ai -Dtest=ConversationPlannerServiceTest,AgentExecutionServiceTest,ChatGroundingServiceTest,AgentChatServiceTest,InternalAgentChatResourceTest test`
- `pnpm --dir dts-copilot-webapp test`

**Step 3: Write minimal implementation**

Finalize names, remove dead clarification-only branches, keep compatibility wrappers only where necessary.

**Step 4: Run test to verify it passes**

Run:
- `mvn -pl dts-copilot-ai -Dtest=ConversationPlannerServiceTest,AgentExecutionServiceTest,ChatGroundingServiceTest,AgentChatServiceTest,InternalAgentChatResourceTest test`
- `pnpm --dir dts-copilot-webapp run typecheck`
- `pnpm --dir dts-copilot-webapp run build:modern`

**Step 5: Commit**

`git commit -m "refactor: complete planner-first chat orchestration"`
