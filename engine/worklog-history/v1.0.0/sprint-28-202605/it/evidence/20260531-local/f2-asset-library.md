# F2 资产库入口收口证据

## 范围

验证 F2-T01~T03 / IT04~IT05:

- `/assets?tab=dashboards`
- `/assets?tab=cards`
- `/assets?tab=collections`
- `/dashboards`
- `/questions`
- `/collections`
- `/dashboards/:id`
- `/questions/:id`
- `/collections/:id`

## RED

命令:

```bash
cd dts-copilot-webapp
pnpm test -- AssetLibraryPage routes.agentWorkspace
```

结果:

- `AssetLibraryPage` 仍直接渲染 `<DashboardsPage />`、`<CardsPage />`、`<CollectionsPage />`,测试失败。
- `DashboardsPage` / `CardsPage` / `CollectionsPage` 没有 `embedded` 模式,测试失败。
- `/dashboards`、`/questions`、`/collections` 仍直接挂旧列表页,没有 `redirectAssetList(...)`,测试失败。

## GREEN

命令:

```bash
cd dts-copilot-webapp
pnpm test -- AssetLibraryPage routes.agentWorkspace
pnpm typecheck
```

结果:

- `pnpm test -- AssetLibraryPage routes.agentWorkspace`: 59 个 test files / 239 个 tests 全部通过。
- `pnpm typecheck`: exit 0。

## Browser Smoke

环境:

```bash
cd dts-copilot-webapp
pnpm exec vite --host 127.0.0.1 --port 3005 --strictPort
```

说明:

- 使用本地 Vite dev server 和浏览器真实渲染。
- 通过 `platformUserStore` 写入本地 smoke token 绕过 standalone 登录检查。
- 本轮没有启动后端服务,列表数据 API 失败日志是预期结果;验证重点是布局层级、tab URL 和路由 redirect。

`/assets?tab=dashboards`:

```json
{
  "url": "http://127.0.0.1:3005/assets?tab=dashboards",
  "pageHeaderCount": 1,
  "headings": ["资产库"],
  "activeTab": "看板",
  "dashboardPage": true
}
```

`/assets?tab=cards`:

```json
{
  "url": "http://127.0.0.1:3005/assets?tab=cards",
  "pageHeaderCount": 1,
  "headings": ["资产库"],
  "activeTab": "卡片"
}
```

`/assets?tab=collections`:

```json
{
  "url": "http://127.0.0.1:3005/assets?tab=collections",
  "pageHeaderCount": 1,
  "headings": ["资产库"],
  "activeTab": "集合"
}
```

旧列表 URL:

```json
[
  {
    "from": "/dashboards",
    "url": "http://127.0.0.1:3005/assets?tab=dashboards",
    "headerCount": 1,
    "activeTab": "看板"
  },
  {
    "from": "/questions",
    "url": "http://127.0.0.1:3005/assets?tab=cards",
    "headerCount": 1,
    "activeTab": "卡片"
  },
  {
    "from": "/collections",
    "url": "http://127.0.0.1:3005/assets?tab=collections",
    "headerCount": 1,
    "activeTab": "集合"
  }
]
```

深链保护:

```json
[
  {
    "url": "http://127.0.0.1:3005/dashboards/1",
    "redirectedToAssets": false
  },
  {
    "url": "http://127.0.0.1:3005/questions/1",
    "redirectedToAssets": false
  },
  {
    "url": "http://127.0.0.1:3005/collections/root",
    "redirectedToAssets": false,
    "headerText": "集合内容"
  }
]
```

## 结论

F2 已完成:

- 资产库 tab 内只渲染 embedded 内容,不再嵌套第二层 `PageContainer` / `PageHeader`。
- `/dashboards`、`/questions`、`/collections` 旧列表入口统一 redirect 到 `/assets?tab=...`。
- 新建、详情、编辑深链仍保留原路由,不会被列表 redirect 误伤。
