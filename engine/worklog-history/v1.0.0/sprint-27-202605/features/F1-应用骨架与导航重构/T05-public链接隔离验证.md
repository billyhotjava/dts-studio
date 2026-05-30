# T05: /public/* 分享链接隔离验证

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

按决策 D10「`/public/card`、`/public/dashboard`、`/public/screen` 保留可访问，外发链接不能断」，确保三类公开分享页在 T01 删除旧页面、T03/T04 重构骨架后仍独立可渲染，**不依赖任何被删除或被重构的工作台页面/组件/导航**。抽出公开页的最小渲染依赖，使其成为自洽子树。

## 技术设计

1. **盘点公开页依赖**：
   - `src/pages/PublicCardPage.tsx`：依赖 `analyticsApi.getPublicCard/queryPublicCard`、`components/charts` 的 `ChartRenderer`、`PageContainer`、`EmptyState`、`ErrorNotice`、`ui/*`、`i18n`。这些均为通用组件，不属被删清单——确认不受 T01 影响。
   - `src/pages/PublicDashboardPage.tsx`：同上 + `analyticsApi.getPublicDashboard/queryPublicDashboardDashcard`。
   - `src/pages/screens/PublicScreenPage.tsx`：自洽（screens 运行时上下文 + `analyticsApi`），且在 `routes.tsx` 中已是 `AppLayout` 之外的全屏路由，天然隔离。

2. **清理对被删/重构页面的残留链接**（关键修复点）：
   - `PublicCardPage.tsx` 行 66-69 面包屑含 `{ label: t(locale, "nav.analyze"), href: "/analyze" }` —— `/analyze` 已被 T01 删除，此链接将指向 404。改为移除该面包屑项，或替换为不引用已删路由的标签（公开页不应导回需鉴权的工作台，建议仅保留「分享」标识）。
   - `PublicDashboardPage.tsx` 行 90-93 同样含 `href: "/analyze"` 面包屑 —— 同样处理。
   - `PublicDashboardPage.tsx` 行 127 `<Link to={`/questions/${cardId}`}>` —— `/questions/:id`（`CardDetailPage`）在 T01 中**保留**（降级供资产库复用），故链接仍有效；但公开分享场景下点进需鉴权页会跳登录。决策：公开页内卡片标题改为纯文本（去掉 `Link`），避免外部访客被导向鉴权墙；若产品要求保留跳转则加注说明。

3. **隔离原则确认**：
   - 公开页不得 import `AppLayout`、`appNavigation`、`CopilotSidebar`、工作台壳组件或任何被删页面模块。用 `grep` 验证。
   - `/public/card`、`/public/dashboard` 当前挂在 `AppLayout` children 下但通过 `isPublicRoute` 跳过鉴权（见 `AppLayout.tsx` 行 66）。配合 T03，公开路由走「极简分支」只渲染 `Outlet`，不挂左导航/双栏/Copilot/MobileTabBar。`/public/screen` 已在 `AppLayout` 之外，无需改动。

4. **i18n 兜底**：移除/替换面包屑后，确认不再引用因 T01 清理而删除的 `nav.analyze` 等 key；`share.note`、`loading`、`common.empty` 等公开页用 key 保留。

## 影响范围

（真实文件/模块/路由/接口）

- `src/pages/PublicCardPage.tsx`（移除 `/analyze` 面包屑链接）
- `src/pages/PublicDashboardPage.tsx`（移除 `/analyze` 面包屑链接、卡片标题去 `/questions/:id` 链接或加说明）
- `src/pages/screens/PublicScreenPage.tsx`（核验自洽，预期无改动）
- `src/layouts/AppLayout.tsx`（公开路由极简渲染分支，与 T03 协同）
- 路由：`/public/card/:uuid`、`/public/dashboard/:uuid`、`/public/screen/:uuid`
- 接口：`analyticsApi.getPublicCard/queryPublicCard/getPublicDashboard/queryPublicDashboardDashcard`、公开 screen 接口（均不变）

## 验证

- [ ] 三类公开链接在未登录（无 token、无 session）状态下可直接渲染图表/看板/大屏，不触发登录跳转。
- [ ] 公开页面包屑/链接不再指向已删除的 `/analyze`，不产生 404 死链。
- [ ] `grep -rn "AppLayout\|appNavigation\|CopilotSidebar" src/pages/PublicCardPage.tsx src/pages/PublicDashboardPage.tsx src/pages/screens/PublicScreenPage.tsx` 无对工作台壳/导航的依赖。
- [ ] 公开页不渲染左导航、双栏工作台、浮动 Copilot、MobileTabBar。
- [ ] `pnpm typecheck` 通过；公开页 e2e/渲染冒烟通过。

## 完成标准

- [ ] 三类 `/public/*` 分享链接在重构后仍正常、独立可访问（D10）。
- [ ] 公开页清除对被删路由的残留链接，最小渲染依赖自洽。
- [ ] 公开路由走极简布局，不挂任何工作台壳依赖。
