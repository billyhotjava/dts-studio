# F7 资产沉淀验证

## 范围

- 画布动作行的 `save-card`、`pin-dashboard`、`export` 接入 `AgentWorkspacePage`。
- `AssetActionModals` 提供存为卡片、钉到看板、导出 CSV/PNG 三个入口。
- `/assets` 资产库入口聚合看板、卡片、集合三个 Tab,导航资产入口指向 `/assets`。

## Mock / Contract

- `assetPayload.test.ts`: `buildCardPayloadFromArtifact` 输出 `name`、`collection_id`、`display`、`dataset_query`、`visualization_settings` 与包含口径/来源/SQL 的描述。
- `assetOperations.test.ts`: draft 产物走 `saveAnalysisDraftCard` 后 `updateCard`;即席产物走 `createCard`;已有 `cardId` 不重复建卡;钉看板执行 `getDashboard` → 追加布局 → `saveDashboard({ dashboard, dashcards })`。
- `dashboardLayout.test.ts`: 空/非空看板追加布局项,row 取 `max(row + size_y)`,不修改原数组。
- `artifactExport.test.ts` / `csv.test.ts` / `download.test.ts`: CSV 转义、统一下载工具、PNG canvas 快照下载均可 mock 验证。
- `AssetLibraryPage.test.ts` / `routes.agentWorkspace.test.ts` / `appNavigation.test.ts`: `/assets` 路由、URL Tab、资产导航入口均已覆盖。

## 命令结果

```bash
pnpm vitest run src/components/asset/assetPayload.test.ts src/components/asset/dashboardLayout.test.ts src/components/asset/assetOperations.test.ts src/components/asset/artifactExport.test.ts src/components/asset/AssetActionModals.test.ts src/lib/csv.test.ts src/lib/download.test.ts src/pages/AssetLibraryPage.test.ts src/pages/AgentWorkspacePage.test.ts src/routes.agentWorkspace.test.ts src/layouts/appNavigation.test.ts
# Test Files 11 passed; Tests 30 passed

pnpm typecheck
# passed

pnpm test
# Test Files 59 passed; Tests 228 passed

pnpm build
# built successfully; existing large chunk warning remains
```

## Runtime Smoke

```text
VITE_CACHE_DIR=.vite-cache pnpm exec vite --host 0.0.0.0 --port 3004 --strictPort --open=false
# Local: http://localhost:3004/

Playwright http://localhost:3004/assets
# rendered sidebar + 资产库 + 看板/卡片/集合 Tabs; local API calls returned expected 500 because no backend session/data service is attached.

Playwright http://localhost:3004/agent-bi
# rendered Agent BI 工作台 and 资产库入口; local API calls returned expected 500 for suggestions/dashboard/card/session fixtures.
```

## Live Contract

Live 入口:`http://localhost:50080`,经 webapp nginx 代理到 analytics。认证使用本机有效 `X-Metabase-Session`,文档不记录 session 值。

### IT07 存为卡片

请求:

```text
POST /api/card
```

请求体要点:

```json
{
  "name": "Sprint27 IT07 卡片 20260531064612",
  "collection_id": null,
  "display": "table",
  "description": "由 Sprint-27 live contract 验证创建; SQL: SELECT 1 AS sprint27_value",
  "dataset_query": {
    "database": 8,
    "type": "native",
    "native": {"query": "SELECT 1 AS sprint27_value"}
  },
  "visualization_settings": {}
}
```

返回与列表校验:

```json
{
  "card": {
    "id": 2,
    "name": "Sprint27 IT07 卡片 20260531064612",
    "visibleInList": true
  }
}
```

校验方式:`GET /api/card` 中按名称查找,结果为 `true`。

### IT08 钉到看板

请求:

```text
POST /api/dashboard
POST /api/dashboard/save
GET /api/dashboard
GET /api/dashboard/{id}
```

关键返回:

```json
{
  "dashboard": {
    "id": 1,
    "name": "Sprint27 IT08 看板 20260531064612",
    "visibleInList": true,
    "hasPinnedCard": true
  }
}
```

校验方式:

- `GET /api/dashboard` 中按名称查找,结果为 `true`。
- `GET /api/dashboard/1` 的 `ordered_cards[]` 包含 `card_id=2`,结果为 `true`。

结论:IT07/IT08 已在 live 容器中完成持久化闭环验证。
