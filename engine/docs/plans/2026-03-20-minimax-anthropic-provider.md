# MiniMax Anthropic Provider Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add first-class MiniMax Anthropic-compatible provider support without breaking existing OpenAI-compatible providers.

**Architecture:** Introduce a provider-client abstraction and a factory that resolves protocol from `providerType` and `baseUrl`. Keep existing OpenAI-compatible behavior intact, add Anthropic-compatible transport for MiniMax, and normalize both into the same result model used by agent execution and streaming chat.

**Tech Stack:** Spring Boot, Java 21, JUnit 5, Mockito, existing Java HTTP client, React frontend unchanged at protocol boundary.

---

### Task 1: Add failing protocol-selection tests

**Files:**
- Create/Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/llm/LlmProviderClientFactoryTest.java`
- Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/config/ProviderTemplateCatalogTest.java`

**Step 1: Write the failing test**
Add tests asserting:
- `providerType=MINIMAX` resolves to Anthropic-compatible client
- `providerType=ANTHROPIC` resolves to Anthropic-compatible client
- `providerType=CUSTOM` with base URL containing `/anthropic` resolves to Anthropic-compatible client
- `ProviderTemplate` contains `MINIMAX`

**Step 2: Run test to verify it fails**
Run: `mvn -pl dts-copilot-ai -Dtest=LlmProviderClientFactoryTest,ProviderTemplateCatalogTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because factory/template do not exist.

**Step 3: Write minimal implementation**
Create protocol enum/factory shell and add `MINIMAX` template.

**Step 4: Run test to verify it passes**
Run the same Maven command and expect PASS.

**Step 5: Commit**
`git commit -m "feat: add minimax provider protocol routing"`

### Task 2: Add failing Anthropic client tests

**Files:**
- Create: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/llm/AnthropicCompatibleClientTest.java`
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/AnthropicCompatibleClient.java`
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/LlmProviderClient.java`

**Step 1: Write the failing test**
Cover:
- `listModels()` hits Anthropic models endpoint shape
- non-stream chat parses Anthropic response text
- stream chat parses SSE chunks and marks completion

**Step 2: Run test to verify it fails**
Run: `mvn -pl dts-copilot-ai -Dtest=AnthropicCompatibleClientTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because client does not exist.

**Step 3: Write minimal implementation**
Implement Anthropic-compatible client and shared interface.

**Step 4: Run test to verify it passes**
Run the same Maven command and expect PASS.

**Step 5: Commit**
`git commit -m "feat: add anthropic compatible llm client"`

### Task 3: Route runtime services through the client factory

**Files:**
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/LlmProviderClientFactory.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/gateway/LlmGatewayService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/llm/gateway/ProviderState.java`
- Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionServiceTest.java`

**Step 1: Write the failing test**
Add/adjust tests so default MiniMax provider uses the new factory and no longer depends on `OpenAiCompatibleClient` concrete type.

**Step 2: Run test to verify it fails**
Run: `mvn -pl dts-copilot-ai -Dtest=AgentExecutionServiceTest,LlmProviderClientFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because services still depend on `OpenAiCompatibleClient`.

**Step 3: Write minimal implementation**
Switch service caches and gateway state from `OpenAiCompatibleClient` to `LlmProviderClient`.

**Step 4: Run test to verify it passes**
Run the same Maven command and expect PASS.

**Step 5: Commit**
`git commit -m "refactor: route llm execution through provider factory"`

### Task 4: Fix provider testing and admin integration

**Files:**
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiConfigResource.java`
- Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigServiceTest.java`
- Create/Modify: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/web/rest/AiConfigResourceTest.java`

**Step 1: Write the failing test**
Assert that provider test uses the resolved protocol client for MiniMax/Anthropic base URLs.

**Step 2: Run test to verify it fails**
Run: `mvn -pl dts-copilot-ai -Dtest=AiConfigServiceTest,AiConfigResourceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because test path still directly instantiates `OpenAiCompatibleClient`.

**Step 3: Write minimal implementation**
Inject/use the factory in config service/resource test path.

**Step 4: Run test to verify it passes**
Run the same Maven command and expect PASS.

**Step 5: Commit**
`git commit -m "fix: use provider protocol in config tests"`

### Task 5: Verify end-to-end compatibility

**Files:**
- Modify if needed: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/llm/OpenAiCompatibleClientTest.java`
- Modify if needed: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/llm/AnthropicCompatibleClientTest.java`

**Step 1: Run focused regression suite**
Run:
`mvn -pl dts-copilot-ai -Dtest=LlmProviderClientFactoryTest,AnthropicCompatibleClientTest,OpenAiCompatibleClientTest,AiConfigServiceTest,AiConfigResourceTest,AgentExecutionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS

**Step 2: Run compile verification**
Run:
`mvn -pl dts-copilot-ai -DskipTests compile`
Expected: PASS

**Step 3: Commit**
`git commit -m "test: cover minimax anthropic provider flow"`
