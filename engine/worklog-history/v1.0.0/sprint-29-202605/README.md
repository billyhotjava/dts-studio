# Sprint-29: dts-platform 指标联邦接入 copilot

**时间**: 2026-05
**前缀**: IF (Indicator Federation)
**状态**: DONE / LIVE_VERIFIED_WITH_LOCAL_FIXTURE
**目标**: 把 dts-platform 治理过的**权威指标**接进 dts-copilot agent-first 单入口——用户问数时优先拿到「口径权威」的指标答案(命中即调平台 API 取值、用 copilot 自己的 echarts 渲染),未命中才退回 AI 现生成 SQL。

## 背景

dts-platform 已有完整治理指标子系统(`/api/governance/indicators*`),指标有权威口径且已编译成 dbt mart 产数。诉求:从平台**取指标**、用 **copilot 自己的可视化组件渲染**(非 iframe 嵌成品报表),延续「治理在 dts-stack、智能在 dts-copilot」。

设计依据:`docs/superpowers/specs/2026-05-31-dts-platform-indicator-federation-design.md`(brainstorm 产出,决策 D1–D7)。

## 范围与核心决策

- **D1 服务联邦**:copilot 实时调平台指标 API 取值,自己渲染;口径单一源在平台。
- **D2/D3 指标优先路由(C,copilot 目标态)**:指标目录同步进 copilot 语义层,复用现有意图路由,优先级 `已发布指标 > 视图/mart > 现生成 SQL`。
- **D4 平台指标业务零改码**:4 个端点(`/indicators`、`/indicators/dashboard`、`/{id}/detail`、`/{id}/drilldown`)+ `IndicatorDto` 字段均已就位;live 401 后只补 dts-platform 服务间认证白名单与配置,不改指标业务逻辑。
- **D5 试用期单机器账号**:复用平台既有 `X-DTS-Service` / `X-DTS-Service-Token` 服务认证契约,简单快部署;⚠️ 生产/多租户前切回用户令牌透传恢复行列权限/密级(显式权宜,非永久)。
- **D6 显式降级**:平台不可达 → 提示 + 一键退回现生成 SQL,不静默。

## 底座依赖

复用 **sprint-27**:F4 活产物画布、F5 口径芯片/乐观执行、F6 溯源、F8 契约;**sprint-28** 已收口路由/资产入口/契约缺口。
⚠️ 若 sprint-27 的 F4/F6/F8 仍有 live 缺口,本 Sprint 落地前需确认已由 sprint-28 压实,否则指标产物会踩同样运行时坑。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 阶段 | 状态 |
|----|---------|---------|--------|------|------|
| F1 | 跨服务接入与指标目录同步(后端) | 5 | P0 | P1 | DONE |
| F2 | 指标产物与渲染(前端) | 4 | P0 | P1 | DONE |
| F3 | 指标优先路由(后端) | 3 | P0 | P2 | DONE |
| F4 | 路由结果接入 agent-first UI(前端) | 3 | P1 | P2 | DONE |
| F5 | 下钻与增强健壮性(全栈) | 4 | P2 | P3 | DONE |
| F6 | PRS固定报表资产库收口(前端) | 1 | P0 | P1b | DONE |

## 依赖顺序

```
P1: F1(后端接入+同步+BFF) ──┬─> F2(前端产物+渲染+资产库浏览)   ← 最快可演示闭环
                            │
P2: F1 ─> F3(指标优先路由) ──┴─> F4(命中→芯片/溯源/乐观执行合流, 复用 sprint-27 F5/F6)
                            └─> 未命中 → 退回现生成 SQL(existing)
P3: F5(drilldown 下钻 / 口径version提醒 / 多候选切换 / 降级·微缓存 / 可观测) 依赖 F2+F4
```

## 落地分段(贴合「尽快简单上手部署」)

- **P1**:F1+F2 —— 机器账号认证 + 目录同步 + 资产库「平台指标」浏览 + 点开调 API 渲染为 Indicator 产物。先上能演示的取数+渲染闭环。
- **P2**:F3+F4 —— 指标优先路由 + 命中接入 agent-first 乐观执行/芯片/权威溯源。
- **P3**:F5 —— 下钻、口径变更提醒、多候选、降级体验、可观测。

## 当前进展(2026-05-31)

- F1 已完成 analytics BFF 与 copilot-ai 目录同步: `dts-copilot-analytics` 新增 `PlatformIndicatorClient`、`/api/platform/indicators*` 端点与 `DTS_PLATFORM_*` 配置;`dts-copilot-ai` 新增 `PlatformIndicatorCatalogClient`、`IndicatorCatalogSyncService`、`IndicatorCatalogStore` 与启动/定时刷新,支持 stale-while-revalidate 与 version change 检测。
- F2 已完成 P1 单窗口闭环:前端 `analyticsApi` 指标客户端、`type:'indicator'` Artifact 模型、`ArtifactCanvas` 指标渲染、资产库「平台指标」tab、搜索、降级提示、取值预览和「交给 Agent」入口均已落地。根据 Sprint-28 的单窗口决策,资产库预览在当前卡片内渲染 Indicator artifact,不再 upsert 到右侧画布。
- F3 已完成指标优先路由: `IndicatorMatcherService` 消费本地已发布指标目录,`AssetBackedPlannerPolicy` 在模板/视图路由前命中 `PUBLISHED_INDICATOR`,并支持 `metric` override 切候选或 `__fallback_generated__` 退回 AI 现生成。
- F4 已完成单窗口接入:对话内展示平台指标徽标、可改指标芯片、权威 trace,并用 `InlineIndicatorPreview` 在消息内取值渲染 Indicator artifact,不恢复右侧预览窗。
- F5 已完成本地增强:指标产物支持同一窗口内 drilldown,analytics 取值端加 TTL 微缓存,AI/analytics 暴露 Micrometer 指标,资产库检测平台指标 version 变化并提示「口径已更新」。
- Live Contract 已从 401 修到服务认证可达:`dts-copilot` 通过 `X-DTS-Service`/`X-DTS-Service-Token` 调 `v223-dts-platform-1` 的指标目录、dashboard、detail、drilldown 均为 HTTP 200。因平台原库无已发布业务指标,本轮创建 `codex_sprint29_live_metric` 本地验证 fixture 完成端到端对账:platform catalog `total=1`,analytics BFF `degraded=false`,dashboard 1 行、detail 2 行、drilldown 3 行。
- 运行时方向已校正:Agent 响应契约现在在同步 `send` 响应中直接带回 `responseKind=PUBLISHED_INDICATOR`、`reportCode`、`targetView`、`dataSurface`、`trace.metricCaliber`;webapp 兼容层不再丢弃这些字段。
- 生产容器路由补齐:`/assets?tab=metrics` 不再被 nginx 静态 `/assets/` 目录 301 带偏,资产库 tab 可直接进入。
- 2026-06-01 收口修复:F6 已把 PRS 固定报表/大屏资产组接回资产库「看板」tab,按 `DBT_SCREEN_TABLE` 主报表折叠 `DBT_SPLIT` 子报表,并把入口修正为 `/agent-bi?fixedReport=...`,避免继续跳到 `/dashboards/new?fixedReportTemplate=...` 的空白创建流。

## 非目标

- 不做指标的创建/编辑(写回平台);copilot 只读消费已发布指标。
- 不 iframe 嵌平台成品报表/大屏;不在 copilot 重算口径(口径恒取平台)。
- dts-platform 不改指标业务代码;仅补服务间认证只读白名单与部署 token 配置。

## 完成标准

- [x] copilot 用机器账号能拉到 dts-platform 已发布指标目录并定期刷新(P1; local fixture live `fetched=1, entries=1`)
- [x] 资产库「平台指标」可浏览,点开调 `dashboard/detail/drilldown` 用 copilot echarts 渲染为 Indicator 产物(P1 live fixture)
- [x] 问数命中已发布指标时,优先走 `PUBLISHED_INDICATOR`;未命中或选择退回时走现生成 SQL(P2 live fixture + mock/local contract)
- [x] 命中指标做成可改芯片(切候选/退回),溯源显示「平台指标X·口径vN」vs「现生成」(P2,复用 sprint-27 F5/F6)
- [x] 平台不可达/超时有显式降级 + 一键退回,不静默不阻断问数(全程 mock/local contract)
- [x] dts-platform 指标业务零改动;服务认证白名单限制为只读指标端点。
- [x] PRS 固定报表/大屏资产在资产库看板 tab 可见,`PRS-FLOWERBIZ-PROJECT-CUSTOMER` 等主报表入口打开 Agent BI 固定报表执行链路,不再被 DBT_SPLIT 子报表挤占。
- [x] `it/README.md` 有本地/Mock/Live fixture 集成验证证据;live contract 已记录服务认证 200、BFF 非降级、Agent 单窗口与浏览器实跑截图。
