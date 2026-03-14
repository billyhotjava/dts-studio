# Copilot System Settings Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an admin-only copilot system settings center that manages site settings, LLM providers, and copilot client API keys.

**Architecture:** `dts-copilot-webapp` talks only to `dts-copilot-analytics`. Analytics enforces superuser access and proxies AI provider/API key operations to `dts-copilot-ai`. AI persists provider data and returns masked provider secrets.

**Tech Stack:** Spring Boot 3.4, Spring MVC, Spring Data JPA, React 19, TypeScript, React Router 7, Node built-in test runner.

---

### Task 1: Harden AI Provider Admin Contract

**Files:**
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/dto/AiProviderConfigRequest.java`
- Create: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/dto/AiProviderConfigResponse.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigService.java`
- Modify: `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiConfigResource.java`
- Test: `dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigServiceTest.java`

**Step 1: Write the failing test**

- Add tests proving:
  - provider responses return `hasApiKey=true` and masked text, not the raw key
  - update with blank API key preserves the stored key

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai -Dtest=AiConfigServiceTest test`
Expected: FAIL because the DTOs and blank-key preservation do not exist yet.

**Step 3: Write minimal implementation**

- Add response DTO with `apiKeyMasked` and `hasApiKey`
- Add request DTO with nullable `apiKey`
- Change service update logic to keep old key when request key is blank
- Change resource to return DTOs instead of JPA entities

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai -Dtest=AiConfigServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git -C /opt/prod/prs/source/dts-copilot add dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/dto/AiProviderConfigRequest.java dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/dto/AiProviderConfigResponse.java dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigService.java dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiConfigResource.java dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigServiceTest.java
git -C /opt/prod/prs/source/dts-copilot commit -m "Copilot配置安全化"
```

### Task 2: Add Analytics Admin Aggregate APIs

**Files:**
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotAdminClient.java`
- Create: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotAdminResource.java`
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotAiClient.java`
- Modify: `dts-copilot-analytics/src/main/resources/application.yml`
- Test: `dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotAdminResourceTest.java`

**Step 1: Write the failing test**

- Add tests proving:
  - superuser can list site settings
  - non-superuser receives `403`
  - provider and API key proxy failures map to stable error messages

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-analytics -Dtest=CopilotAdminResourceTest test`
Expected: FAIL because the aggregate resource does not exist.

**Step 3: Write minimal implementation**

- Add a client wrapper that calls AI provider/API key admin endpoints with server-side `copilot.admin-secret`
- Add admin endpoints for:
  - `GET/PUT /api/admin/copilot/settings/site`
  - `GET/POST/PUT/DELETE /api/admin/copilot/providers`
  - `POST /api/admin/copilot/providers/{id}/test`
  - `GET/POST/PUT/DELETE /api/admin/copilot/api-keys`
- Reuse `AnalyticsSessionService.resolveUser(request)` and `isSuperuser()`

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-analytics -Dtest=CopilotAdminResourceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git -C /opt/prod/prs/source/dts-copilot add dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotAdminClient.java dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotAdminResource.java dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/CopilotAiClient.java dts-copilot-analytics/src/main/resources/application.yml dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/web/rest/CopilotAdminResourceTest.java
git -C /opt/prod/prs/source/dts-copilot commit -m "聚合Copilot系统配置接口"
```

### Task 3: Extend Webapp API Client

**Files:**
- Modify: `dts-copilot-webapp/src/api/analyticsApi.ts`
- Create: `dts-copilot-webapp/tests/copilotSettingsApi.test.ts`

**Step 1: Write the failing test**

- Add tests proving:
  - provider payloads preserve blank API key as undefined on update
  - create/rotate API key responses surface the one-time raw key
  - site settings payloads map cleanly to UI state

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/copilotSettingsApi.test.ts --bundle --platform=node --format=esm --outfile=/tmp/copilotSettingsApi.test.mjs && node --test /tmp/copilotSettingsApi.test.mjs`
Expected: FAIL because the API helpers do not exist.

**Step 3: Write minimal implementation**

- Add typed helpers for site settings, provider CRUD/test, and API key CRUD/rotate
- Keep browser requests on analytics routes only

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/copilotSettingsApi.test.ts --bundle --platform=node --format=esm --outfile=/tmp/copilotSettingsApi.test.mjs && node --test /tmp/copilotSettingsApi.test.mjs`
Expected: PASS

**Step 5: Commit**

```bash
git -C /opt/prod/prs/source/dts-copilot add dts-copilot-webapp/src/api/analyticsApi.ts dts-copilot-webapp/tests/copilotSettingsApi.test.ts
git -C /opt/prod/prs/source/dts-copilot commit -m "补充配置页API客户端"
```

### Task 4: Build the System Settings Page

**Files:**
- Create: `dts-copilot-webapp/src/pages/admin/CopilotSettingsPage.tsx`
- Create: `dts-copilot-webapp/src/pages/admin/CopilotSettingsPage.css`
- Modify: `dts-copilot-webapp/src/routes.tsx`
- Modify: `dts-copilot-webapp/src/layouts/AppLayout.tsx`
- Modify: `dts-copilot-webapp/src/i18n.ts`
- Test: `dts-copilot-webapp/tests/copilotSettingsPage.test.ts`

**Step 1: Write the failing test**

- Add tests proving:
  - the admin nav entry appears for privileged users
  - provider edit leaves the API key input blank by default
  - create/rotate flow shows raw key once in a success panel

**Step 2: Run test to verify it fails**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/copilotSettingsPage.test.ts --bundle --platform=node --format=esm --outfile=/tmp/copilotSettingsPage.test.mjs && node --test /tmp/copilotSettingsPage.test.mjs`
Expected: FAIL because the page and route do not exist.

**Step 3: Write minimal implementation**

- Add the page with three cards:
  - site settings
  - provider management
  - API key management
- Add route `/admin/settings/copilot`
- Add admin nav item in the sidebar

**Step 4: Run test to verify it passes**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/copilotSettingsPage.test.ts --bundle --platform=node --format=esm --outfile=/tmp/copilotSettingsPage.test.mjs && node --test /tmp/copilotSettingsPage.test.mjs`
Expected: PASS

**Step 5: Commit**

```bash
git -C /opt/prod/prs/source/dts-copilot add dts-copilot-webapp/src/pages/admin/CopilotSettingsPage.tsx dts-copilot-webapp/src/pages/admin/CopilotSettingsPage.css dts-copilot-webapp/src/routes.tsx dts-copilot-webapp/src/layouts/AppLayout.tsx dts-copilot-webapp/src/i18n.ts dts-copilot-webapp/tests/copilotSettingsPage.test.ts
git -C /opt/prod/prs/source/dts-copilot commit -m "增加Copilot系统配置页"
```

### Task 5: Run Final Verification

**Files:**
- Modify: `worklog/v1.0.0/sprint-9/README.md`
- Modify: `worklog/v1.0.0/sprint-queue.md`

**Step 1: Run backend tests**

Run: `cd /opt/prod/prs/source/dts-copilot && mvn -pl dts-copilot-ai,dts-copilot-analytics test`
Expected: PASS

**Step 2: Run frontend tests**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && pnpm run typecheck`
Expected: PASS

**Step 3: Run focused Node tests**

Run: `cd /opt/prod/prs/source/dts-copilot/dts-copilot-webapp && node node_modules/.pnpm/esbuild@0.25.12/node_modules/esbuild/bin/esbuild tests/copilotSettingsApi.test.ts --bundle --platform=node --format=esm --outfile=/tmp/copilotSettingsApi.test.mjs && node --test /tmp/copilotSettingsApi.test.mjs`
Expected: PASS

**Step 4: Update sprint status**

- Mark completed tasks in `worklog/v1.0.0/sprint-9/README.md`
- Update `worklog/v1.0.0/sprint-queue.md`

**Step 5: Commit**

```bash
git -C /opt/prod/prs/source/dts-copilot add worklog/v1.0.0/sprint-9/README.md worklog/v1.0.0/sprint-queue.md
git -C /opt/prod/prs/source/dts-copilot commit -m "完成Copilot系统配置功能"
```

