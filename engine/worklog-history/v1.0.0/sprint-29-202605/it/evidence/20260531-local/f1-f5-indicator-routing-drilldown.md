# 2026-05-31 Local Evidence: Sprint-29 F1-F5 Indicator Federation

## Scope

- F1: analytics BFF + copilot-ai indicator catalog sync/store.
- F2: asset library platform indicator browsing/preview.
- F3: published-indicator-first planner route and metric override fallback.
- F4: single-window inline indicator preview, platform badge, editable metric chip contract.
- F5: drilldown, caliber version reminder, value micro-cache, metrics.

## Commands

```bash
mvn -pl dts-copilot-ai -Dtest=IndicatorCatalogMapperTest,IndicatorCatalogStoreTest,IndicatorCatalogSyncServiceTest,IndicatorMatcherServiceTest,AssetBackedPlannerPolicyTest,CopilotChatContractTest test
```

Result: PASS, 36 tests.

```bash
mvn -pl dts-copilot-analytics -Dtest=PlatformIndicatorClientTest,PlatformIndicatorResourceTest test
```

Result: PASS, 7 tests.

```bash
mvn test
```

Result: PASS. Reactor modules succeeded:
- `dts-copilot-ai`: 191 tests.
- `dts-copilot-analytics`: 106 tests.

```bash
pnpm test
```

Result: PASS, 65 files / 266 tests.

```bash
pnpm typecheck
```

Result: PASS.

```bash
pnpm build
```

Result: PASS. Vite build completed; existing chunk-size warning remains.

```bash
docker compose config >/tmp/dts-copilot-compose-rendered.yml
rg -n "DTS_PLATFORM_BASE_URL|DTS_PLATFORM_VALUE_CACHE_TTL_SECONDS|PLATFORM_INDICATOR_SYNC_ENABLED|copilot-ai|copilot-analytics" /tmp/dts-copilot-compose-rendered.yml
```

Result: PASS. Compose renders platform base URL/token settings, AI sync settings, and analytics value-cache TTL settings.

## Live Contract

Current local platform container is present and healthy:

```bash
docker ps --format '{{.Names}} {{.Status}} {{.Ports}}' | rg 'dts-copilot|v223-dts-platform'
```

Result: `v223-dts-platform-1` and dts-copilot services are healthy.

Original direct platform attempts proved the previous assumption was wrong:

```bash
GET http://127.0.0.1:8081/api/governance/indicators?status=已发布
```

Result:
- Without bearer token: 401.
- With existing OIDC client-credentials token from the running platform container env: 401.

Root cause:
- dts-platform uses `PortalOpaqueTokenIntrospector`, so Keycloak client-credentials JWTs are not accepted as portal access tokens.
- The existing service-to-service contract is `X-DTS-Service` + `X-DTS-Service-Token`.

Fix applied:
- dts-platform service-auth filter now allows `dts-copilot` only for read-only governance indicator endpoints.
- dts-copilot AI/analytics clients prefer service headers, with token fallback `DTS_INBOUND_FROM_COPILOT -> DTS_INBOUND_FROM_ANALYTICS -> DTS_ADMIN_SERVICE_TOKEN`.
- dts-copilot compose attaches AI/analytics to the external `dts-core` network so `http://dts-platform:8081` resolves.

Live checks after the fix:

```bash
GET /api/governance/indicators?status=已发布
Header: X-DTS-Service=dts-copilot
```

Result: HTTP 200, `data.total=0`.

```bash
GET /api/governance/indicators/dashboard?days=30
Header: X-DTS-Service=dts-copilot
```

Result: HTTP 200, `data.total=0`, empty `indicators`.

```bash
GET http://127.0.0.1:50092/api/platform/indicators?page=0&size=1
Authorization: Bearer <temporary verification API key>
```

Result: HTTP 200, `{"items":[],"syncedAt":null,"degraded":false,"degradedReason":null}`.

```bash
GET http://127.0.0.1:50092/api/platform/indicators/dashboard?days=30
Authorization: Bearer <temporary verification API key>
```

Result: HTTP 200, `degraded=false`, empty cols/rows.

Conclusion at this checkpoint: IT01 live auth was DONE. IT04 still needed a published indicator sample because the platform database initially had `total=0`.

## Live Fixture Completion Pass

To close the runtime gap without changing dts-platform indicator business code, a local verification fixture was inserted into the running platform database:

- indicator id: `29000000-0000-4000-8000-000000000029`
- indicator code: `codex_sprint29_live_metric`
- indicator name: `Sprint29 验证指标`
- source table: `public.codex_sprint29_indicator_fixture`
- measure field: `value`
- dimension field: `dept`
- status: `PUBLISHED`
- visibility: `owner_dept = null`, `data_level = PUBLIC`

Direct dts-platform service-auth checks:

```bash
GET /api/governance/indicators?status=PUBLISHED&page=0&size=5
```

Result: HTTP 200, `data.total=1`, first item `code=codex_sprint29_live_metric`.

```bash
GET /api/governance/indicators/29000000-0000-4000-8000-000000000029/detail?days=30
```

Result: HTTP 200, current value `300`, trend rows `260 -> 300`, alert level `GREEN`.

```bash
GET /api/governance/indicators/29000000-0000-4000-8000-000000000029/drilldown?dimension=dept
```

Result: HTTP 200, rows:
- `north`, metric value `200`, row count `2`
- `south`, metric value `60`, row count `1`
- `east`, metric value `40`, row count `1`

Analytics BFF checks:

```bash
GET http://127.0.0.1:50092/api/platform/indicators
```

Result: HTTP 200, `degraded=false`, `items.length=1`, `dimensionFields=["dept"]`.

```bash
GET http://127.0.0.1:50092/api/platform/indicators/dashboard?days=30
GET http://127.0.0.1:50092/api/platform/indicators/{id}/detail?days=30
GET http://127.0.0.1:50092/api/platform/indicators/{id}/drilldown?dimension=dept
```

Result:
- dashboard: `degraded=false`, `rows=1`
- detail: `degraded=false`, cols `date,value,alertLevel`, `rows=2`
- drilldown: `degraded=false`, cols `dimension,metric_value,row_count`, `rows=3`

AI catalog sync:

```text
Platform indicator catalog synced: fetched=1, entries=1, changed=0
```

Agent contract:

```bash
POST http://127.0.0.1:50091/api/ai/agent/chat/send
message="查询 Sprint29 验证指标"
```

Result: HTTP 200 with:
- `responseKind=PUBLISHED_INDICATOR`
- `reportCode=codex_sprint29_live_metric`
- `targetView=indicator:codex_sprint29_live_metric`
- `dataSurface=L3_PUBLISHED_INDICATOR`
- `qualityLevel=HIGH`
- `trace.metricCaliber.name=Sprint29 验证指标`
- `trace.metricCaliber.ontologyRef=29000000-0000-4000-8000-000000000029`

Browser check:

- URL: `http://127.0.0.1:50080/assets?tab=metrics`
- Asset library showed `Sprint29 验证指标`, platform metric count `1`, local metric count `0`.
- `预览取值` rendered an inline Indicator artifact with trend rows.
- `下钻` rendered `north/south/east` rows from the live platform drilldown endpoint.
- `交给 Agent` opened `/agent-bi?...source=asset-library-metric&submit=1`, rendered the result in the single Agent window, and did not reintroduce the removed right-side preview surface.
- Screenshot: `../../assets/sprint29-agent-single-window.png`

Additional defects fixed during this pass:

- dts-copilot platform clients now request `status=PUBLISHED`, matching dts-platform's persisted enum value instead of the Chinese display label.
- analytics BFF now maps platform-native `dashboard.indicators[]`, `detail.trend[]`, and drilldown `data[]` payloads into non-empty `cols/rows`.
- `dimensionFields` now parses JSON-array text such as `["dept"]` instead of exposing it as one dirty string.
- synchronous Agent chat responses now preserve `responseKind/reportCode/targetView/dataSurface/qualityLevel/sourceRefs/trace` so the first rendered message has the same contract as the persisted session.
- webapp legacy chat normalization now preserves the same contract fields.
- nginx now treats `/assets` and `/assets/` as SPA routes, preventing the production static `/assets/` directory from redirecting `/assets?tab=metrics` away from port 50080.

## Post-Fix Regression

```bash
mvn -pl dts-copilot-ai,dts-copilot-analytics -Dtest='PlatformIndicatorClientTest' test
```

Result: PASS, 7 tests.

```bash
mvn -pl dts-platform -am -Dtest=ServiceDependencyAuthenticationFilterTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: PASS, 23 tests.

```bash
docker compose config --quiet
```

Result: PASS.

```bash
mvn -pl dts-copilot-ai,dts-copilot-analytics -DskipTests package
docker compose build copilot-ai copilot-analytics
docker compose up -d --no-deps copilot-ai copilot-analytics
```

Result: PASS. `dts-copilot-ai` and `dts-copilot-analytics` healthy; analytics has non-empty `DTS_PLATFORM_SERVICE_TOKEN`, resolves `dts-platform`, and connects to `dts-platform:8081`.

Final BFF smoke after image rebuild:
- `GET /api/platform/indicators?page=0&size=1` -> HTTP 200, `degraded=false`, `items=[]`.
- `GET /api/platform/indicators/dashboard?days=30` -> HTTP 200, `degraded=false`, empty rows.

```bash
git diff --check
```

Result: PASS for `dts-copilot`, `dts-stack`, and `/opt/prod/s10/v2.2.3`.
