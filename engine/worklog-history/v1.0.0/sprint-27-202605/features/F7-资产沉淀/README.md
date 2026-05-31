# F7: 资产沉淀(存卡片/钉看板/资产库)

**优先级**: P1
**状态**: DONE

## 目标

打通对话闭环的最后一步(设计 spec §6 闭环第 6 步「📌 存为资产」):对话产出的「活产物画布」结果,能一键 **存为卡片**、**钉到看板**、**导出**;同时把旧 Metabase 范式 BI 的 `dashboards / cards / collections` 按决策 **D5** 降级整合成 **「📁资产库」二级入口**(不再是一级菜单,见 spec §5-B「沉淀为资产类型」)。让「聊天问出来的结果存得进看板、BI 里的资产仍可浏览管理」,消除 spec §1 诊断的「两个平行世界」问题。

## 背景

- spec §4 状态二:活产物画布顶部动作行固定为 `[存为卡片] [钉到看板] [</> SQL·溯源] [导出]`。其中 SQL/溯源由 F5/F6 负责,本 Feature 负责 **存卡片 / 钉看板 / 导出** 三个沉淀动作 + 资产库浏览入口。
- 现状盘点(真实代码):
  - 建卡片接口已存在 —— `analyticsApi.createCard(body)` → `POST /api/analytics/card`;集合列表 `listCollections()` → `GET /api/analytics/collection`。Copilot 草稿另有专用晋升接口 `saveAnalysisDraftCard(id)` → `POST /api/analytics/analysis-drafts/{id}/save-card`,返回 `{ draft, card }`。
  - 建/存看板接口已存在 —— `createDashboard(body)` → `POST /api/analytics/dashboard`、`saveDashboard(body)` → `POST /api/analytics/dashboard/save`;看板布局项类型 `DashboardCard { row, col, size_x, size_y, card }`(`src/api/types.ts`),`DashboardGrid` 内部转成 react-grid-layout 的 `{ i, x, y, w, h }`。
  - 浏览页面均已存在且独立可用:`CardsPage.tsx`、`DashboardsPage.tsx`、`CollectionsPage.tsx`、`CollectionItemsPage.tsx`、`DashboardDetailPage.tsx`(F1 已保留这些页面组件)。
  - 资产中心模型 `pages/queryAssetCenterModel.ts` 已提供 cards+drafts 归一化/过滤/分组能力,可被资产库直接复用。
  - **无统一导出工具**:`createObjectURL + a.download` 的下载样板在 `screens/**` 多处重复(如 `useExportHandlers.ts` 的 `downloadBlob`);report-factory 有 `getReportRunExportUrl(id, format)`。结果集导出 CSV / 图表导出图片目前没有公用工具,需 T04 新增。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 存为卡片 | P1 | DONE | F4 画布、F5 回答 |
| T02 | 钉到看板 | P1 | DONE | F4 画布、F5 回答、T01 |
| T03 | 资产库二级入口 | P1 | DONE | F1(保留页面组件) |
| T04 | 导出 | P1 | DONE | F4 画布、F5 回答 |

## 完成标准

- [x] 画布结果卡动作行的 `[存为卡片] [钉到看板] [导出]` 三个动作全部可用,均从画布当前产物取数据
- [x] 「存为卡片」可命名 + 选集合,写入产物 spec / SQL / 命中口径,成功后可在资产库看到该卡片
- [x] 「钉到看板」可选已有看板或新建,产物作为一个布局项追加进看板并持久化
- [x] 「📁资产库」作为二级入口存在(非一级菜单),以 Tab/分组聚合看板/卡片/集合三类资产的浏览能力
- [x] 「导出」支持结果集 CSV 与图表图片两种;复用统一导出工具,无重复样板
- [x] `pnpm typecheck`、`pnpm test`、`pnpm build` 全绿;新增逻辑(组装卡片/看板 payload、CSV 序列化、布局项追加)有单测覆盖

## 实施摘要

- 新增 `AssetActionModals` 接管画布动作行的存卡片、钉看板、导出入口,并在 `AgentWorkspacePage` 回写成功后的 `cardId`,避免重复建卡。
- 新增 `assetPayload.ts`、`assetOperations.ts`、`dashboardLayout.ts`、`artifactExport.ts`、`lib/csv.ts`、`lib/download.ts`,把卡片 payload、看板布局追加、CSV 序列化和下载行为拆为可测单元。
- 新增 `/assets` 的 `AssetLibraryPage`,用 URL tab 聚合 `DashboardsPage` / `CardsPage` / `CollectionsPage`,导航资产入口指向 `/assets`。

## 验证证据

- `it/evidence/20260531-local/f7-asset-actions.md`
