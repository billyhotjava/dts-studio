# F2: 指标产物与渲染(前端)

**优先级**: P0
**状态**: READY

## 目标

把 dts-platform 的治理指标做成 copilot **自己的可视化产物**——而不是 iframe 嵌平台成品报表。具体:

- 扩展 sprint-27 F4 活产物画布的 `Artifact`,新增 `type: 'indicator'`,在 `ArtifactSpec` 内承载指标元数据(口径 `definition`/`version`/`expressionSql`/`dimensionFields`/`timeGrain`)+ 取值序列 + 下钻能力标记。
- 复用现有 `ChartRenderer`(echarts 封装)与 `DataTable`,按 `timeGrain`/`dimensionFields` 渲染趋势/明细;不支持的形态回退表格——不新增图表实现。
- 资产库(sprint-27 F7 `AssetLibraryPage`)新增「平台指标」分组(tab),调 F1 BFF 列目录;点开某指标 → 召唤为 Indicator 产物(调取值 → upsert 进画布)。
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
| T01 | Indicator 产物类型(扩展 `artifact.ts` + 对齐 `useArtifactStore`) | P0 | READY | F1(契约字段) |
| T02 | 指标渲染器(复用 `ChartRenderer`/`DataTable`,按 timeGrain/dimensionFields 渲染,回退表格) | P0 | READY | T01 |
| T03 | 资产库「平台指标」分组(`AssetLibraryPage` 新 tab,列目录 + 点开召唤为产物) | P0 | READY | T01, T02, T04 |
| T04 | 前端指标 API 客户端(`analyticsApi`/`api/modules` 增目录·取值方法,带降级) | P0 | READY | F1(BFF 端点) |

## 完成标准

- [ ] `Artifact` 支持 `type: 'indicator'`,`ArtifactSpec` 含 `indicator` 元数据块(name/definition/version/expressionSql/dimensionFields/timeGrain)+ 取值 `dataset` + `drilldownEnabled` 标记;`useArtifactStore.upsert` 对同 id 原地刷新(取新值不新增产物)。
- [ ] `ArtifactCanvas` 能渲染 indicator 产物:有时间序列→趋势图(line/area),纯明细/多维→表格;`display` 不支持时回退 `DataTable`,不报错。
- [ ] `AssetLibraryPage` 新增「平台指标」tab,调 F1 BFF 列出已发布指标目录;点开任一指标 → 调取值端点 → 包装成 Indicator 产物 upsert 进画布。
- [ ] `analyticsApi` 暴露指标目录列表 / 取值(dashboard·detail·drilldown)方法,接 F1 的 `/api/analytics/...` BFF 端点;平台不可达/超时返回显式降级态(空目录 + 提示),UI 不抛未捕获异常。
- [ ] 单测覆盖:产物类型构造(T01)、渲染分支(T02)、资产库 tab 列表渲染与召唤回调(T03)、客户端降级路径(T04)。
- [ ] 不改动 dts-copilot-webapp 之外的源代码;dts-platform 零改码。
