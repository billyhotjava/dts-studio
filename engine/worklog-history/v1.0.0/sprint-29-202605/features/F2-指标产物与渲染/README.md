# F2: 指标产物与渲染(前端)

**优先级**: P0
**状态**: DONE

## 目标

把 dts-platform 的治理指标做成 copilot **自己的可视化产物**——而不是 iframe 嵌平台成品报表。具体:

- 扩展 sprint-27 F4 活产物画布的 `Artifact`,新增 `type: 'indicator'`,在 `ArtifactSpec` 内承载指标元数据(口径 `definition`/`version`/`expressionSql`/`dimensionFields`/`timeGrain`)+ 取值序列 + 下钻能力标记。
- 复用现有 `ChartRenderer`(echarts 封装)与 `DataTable`,按 `timeGrain`/`dimensionFields` 渲染趋势/明细;不支持的形态回退表格——不新增图表实现。
- 资产库(sprint-27 F7 `AssetLibraryPage`)新增「平台指标」分组(tab),调 F1 BFF 列目录;点开某指标 → 调取值并在单窗口资产库卡片内渲染 Indicator 产物。
- 新增前端 API 客户端方法对接 F1 的 BFF 端点(指标目录列表 / 取值 dashboard·detail·drilldown),带降级:平台不可达时 UI 显式提示而非崩溃。

本 Feature 属 **P1 阶段**(取值+渲染,浏览召唤,不碰复杂路由),依赖 **F1**(后端 BFF 端点)。落地后即构成「资产库浏览 → 点开 → 调平台 API → copilot echarts 渲染」的最快可演示闭环。

## 范围边界

- 只读消费已发布指标;不做创建/编辑/写回平台。
- 不在前端重算口径;`definition`/`expressionSql` 仅做展示与溯源,数值恒取平台。
- 路由命中后把指标包装成芯片/溯源的 UI 合流属 **F4**(P2),本 Feature 仅提供产物类型与渲染底座 + 资产库主动召唤路径。
- `drilldown` 的交互式下钻 UI 属 **F5**(P3);本 Feature 仅在产物类型上预留 `drilldownEnabled` 能力标记 + 客户端方法,不做交互编排。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | Indicator 产物类型(扩展 `artifact.ts` + 稳定 id/trace) | P0 | DONE | F1(契约字段) |
| T02 | 指标渲染器(复用 `ChartRenderer`/`DataTable`,按 timeGrain/dimensionFields 渲染,回退表格) | P0 | DONE | T01 |
| T03 | 资产库「平台指标」分组(`AssetLibraryPage` 新 tab,列目录 + 点开单窗口预览/交给 Agent) | P0 | DONE | T01, T02, T04 |
| T04 | 前端指标 API 客户端(`analyticsApi`/`api/modules` 增目录·取值方法,带降级) | P0 | DONE | F1(BFF 端点) |

## 当前进展(2026-05-31)

- T01/T02 已完成:`ArtifactType` 增 `indicator`,新增 `indicatorArtifact`/`makeIndicatorArtifactId`/`resolveIndicatorDisplay`;`ArtifactCanvas` 可渲染「平台指标产物」并展示「平台指标 · 指标名 · 口径 vN」。
- T03 已完成资产库 tab 的浏览、搜索、降级提示、取值预览与召唤入口:`/assets?tab=metrics` 读取平台指标目录和本地 Metric Lens;「预览取值」调用 `getPlatformIndicatorDetail` 后构造 `type:'indicator'` artifact 并在当前资产卡片内用 `ArtifactCanvas` 渲染;「交给 Agent」链接到 `/agent-bi?prompt=...&metric=...&submit=1`。
- T04 已完成前端客户端与降级契约:`analyticsApi.listPlatformIndicators`、`getPlatformIndicatorDashboard`、`getPlatformIndicatorDetail`、`getPlatformIndicatorDrilldown` 已挂载,平台不可达/错误时返回 `degraded:true`。
- 单窗口范围调整:根据 Sprint-28 决策,本 Task 不再要求右侧画布 upsert;F4/F5 后续若需要 Agent 命中结果沉淀,应在对话脊柱/单窗口结果面复用 `indicatorArtifact`,而不是恢复双窗口。

## 完成标准

- [x] `Artifact` 支持 `type: 'indicator'`,`ArtifactSpec` 含 `indicator` 元数据块(name/definition/version/expressionSql/dimensionFields/timeGrain)+ 取值 `dataset`。
- [x] `ArtifactCanvas` 能渲染 indicator 产物:有时间序列→趋势图(line),纯明细/多维→表格,不报错。
- [x] `AssetLibraryPage` 新增「平台指标」tab,调 F1 BFF 列出已发布指标目录;点开任一指标 → 调取值端点 → 包装成 Indicator 产物在单窗口资产卡片内预览。
- [x] `analyticsApi` 暴露指标目录列表 / 取值(dashboard·detail·drilldown)方法,接 F1 的 `/api/analytics/...` BFF 端点;平台不可达/超时返回显式降级态(空目录 + 提示),UI 不抛未捕获异常。
- [x] 单测覆盖:产物类型构造(T01)、渲染分支(T02)、资产库 tab 列表渲染与召唤回调(T03)、客户端降级路径(T04)。
- [x] F2 前端产物渲染不改 dts-platform 指标业务代码;跨服务认证只读白名单由 F1 统一收口。
