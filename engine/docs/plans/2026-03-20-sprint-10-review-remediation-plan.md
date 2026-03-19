# Sprint 10 Review Remediation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close the end-to-end gaps found in sprint-10 review so the shipped copilot path actually uses the new business grounding, feedback, and suggestion capabilities.

**Architecture:** Keep a single production copilot path based on `webapp -> analytics session proxy -> ai agent chat`. Wire the sprint-10 grounding assets into that path, align feedback/suggestions with the AI REST surface, and remove the misleading standalone copilot execute side path from the runtime/UI surface. Preserve the existing analytics session and dataset permission model instead of introducing a second query execution contract.

**Tech Stack:** Spring Boot 3, JPA, Liquibase-backed metadata, React 19 + Vite, TypeScript, JUnit 5.

---

### Task 1: Register Sprint Follow-up

**Files:**
- Create: `worklog/v1.0.0/sprint-10/tasks/BG-12-review-remediation-and-runtime-wiring.md`

**Step 1: Write the task note**

Capture the four concrete fixes from review:
- runtime grounding wiring
- feedback endpoint alignment
- welcome suggestions integration
- copilot execute path cleanup

**Step 2: Commit**

```bash
git add worklog/v1.0.0/sprint-10/tasks/BG-12-review-remediation-and-runtime-wiring.md
git commit -m "补充 sprint10 review 修复任务"
```

### Task 2: Lock Failing Tests

**Files:**
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionServiceTest.java`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/web/rest/Nl2SqlResourceTest.java`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/IntentRouterServiceTest.java`
- Test: `dts-copilot-webapp/tests/welcomeSuggestions.test.ts`
- Test: `dts-copilot-webapp/tests/chatFeedbackApi.test.ts`

**Step 1: Write failing tests**

Add tests that prove:
- agent runtime includes routed context instead of generic-only prompt
- NL2SQL feedback endpoint path is `/api/ai/nl2sql/feedback`
- welcome card consumes backend suggestions instead of only static data
- router asks for clarification on low-confidence single-hit questions

**Step 2: Run tests to verify they fail**

Run:

```bash
cd dts-copilot
./mvnw -pl dts-copilot-ai -Dtest=AgentExecutionServiceTest,Nl2SqlResourceTest,IntentRouterServiceTest test
cd dts-copilot-webapp && pnpm exec vitest run tests/welcomeSuggestions.test.ts tests/chatFeedbackApi.test.ts
```

Expected: failures showing missing wiring/path behavior.

### Task 3: Wire Grounding into the AI Chat Path

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/IntentRouterService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/SemanticPackService.java`

**Step 1: Implement minimal runtime context assembly**

At chat execution time:
- route the user message
- load domain semantic pack context
- carry template match info when available
- inject a compact grounding block into the chat system prompt

**Step 2: Tighten router clarification behavior**

Make low-confidence/single-hit ambiguous prompts return `needsClarification=true`.

**Step 3: Run AI tests**

Run:

```bash
cd dts-copilot
./mvnw -pl dts-copilot-ai -Dtest=AgentExecutionServiceTest,Nl2SqlResourceTest,IntentRouterServiceTest,TemplateMatcherServiceTest test
```

Expected: PASS.

### Task 4: Align Feedback and Suggestions with the Shipped Web UI

**Files:**
- Modify: `dts-copilot-webapp/src/api/analyticsApi.ts`
- Modify: `dts-copilot-webapp/src/components/copilot/WelcomeCard.tsx`
- Modify: `dts-copilot-webapp/src/components/copilot/CopilotChat.tsx`
- Modify: `dts-copilot-webapp/src/components/copilot/FeedbackButtons.tsx`

**Step 1: Add suggestion API and use it in the welcome card**

Load `/api/ai/nl2sql/suggestions`, fall back to curated defaults only when the API is unavailable.

**Step 2: Align feedback submission**

Point the web client at `/api/ai/nl2sql/feedback` and include whatever routing/template metadata is already available from the current message/session state.

**Step 3: Run web tests**

Run:

```bash
cd dts-copilot/dts-copilot-webapp
pnpm exec vitest run tests/welcomeSuggestions.test.ts tests/chatFeedbackApi.test.ts
pnpm run typecheck
```

Expected: PASS.

### Task 5: Clean Up the Unused Copilot Execute Side Path

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotQueryResource.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotQueryService.java`
- Test: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/CopilotQueryServiceTest.java`

**Step 1: Remove or de-emphasize the unsafe side path**

Either delete the unused endpoint if no runtime caller exists, or make it resolve analytics session + permission checks before execution. Do not keep a header-only permission bypass path.

**Step 2: Update tests**

Ensure the remaining tests assert only behavior that is still exposed.

**Step 3: Run analytics tests**

Run:

```bash
cd dts-copilot
./mvnw -pl dts-copilot-analytics -Dtest=CopilotQueryServiceTest test
```

Expected: PASS.

### Task 6: Final Verification

**Files:**
- Verify only

**Step 1: Run the focused verification set**

```bash
cd dts-copilot
./mvnw -pl dts-copilot-ai,dts-copilot-analytics -Dtest=AgentExecutionServiceTest,Nl2SqlResourceTest,IntentRouterServiceTest,TemplateMatcherServiceTest,CopilotQueryServiceTest test
cd dts-copilot-webapp
pnpm exec vitest run tests/welcomeSuggestions.test.ts tests/chatFeedbackApi.test.ts
pnpm run typecheck
pnpm run build:modern
```

Expected: all green.

**Step 2: Commit**

```bash
git add dts-copilot-ai dts-copilot-analytics dts-copilot-webapp docs/plans worklog/v1.0.0/sprint-10/tasks
git commit -m "收口 sprint10 review 修复"
```
