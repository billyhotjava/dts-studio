# T01: drilldown 交互下钻(画布动作 → 原地刷新产物)

**优先级**: P2
**状态**: READY
**依赖**: F2(Indicator 产物 / CanvasActions)、F4(命中产物挂入画布)

## 目标

在 Indicator 产物画布上提供「下钻」动作:用户选一个维度(可选周期)→ copilot 调平台 `GET /indicators/{id}/drilldown?dimension=&period=` 拿权威下钻聚合 → **复用 sprint-27 F4 的 upsert 机制,在同一个 `artifactId` 上原地刷新产物**(不新增产物、不丢对话上下文)。维度/周期候选取自指标定义的 `IndicatorDto.dimensionFields` 与 `IndicatorDto.timeGrain`。

## 技术设计

### 1. 前端:新增「下钻」画布动作

- 在 `src/types/artifact.ts` 的 `CanvasActionType` 联合类型新增 `"drilldown"`:
  ```ts
  export type CanvasActionType =
    | "save-card"
    | "pin-dashboard"
    | "trace-sql"
    | "export"
    | "drilldown";
  ```
- 在 `src/components/canvas/CanvasActions.tsx` 的 `ACTIONS` 列表追加 `{ action: "drilldown", icon: <ActionIcon label="⤵" />, label: "下钻" }`,并仅对 `artifact.type === "indicator"` 启用(其余类型放进 `disabledActions`)。
- 下钻前需让用户选维度/周期:在产物面板渲染一个轻量选择器,**选项来自该指标的 `dimensionFields`(维度)与 `timeGrain`(周期粒度)**。这两个字段在 F2 包装 Indicator 产物时已从 `IndicatorDto` 透传到 `Artifact.spec`(F5 复用,不重复拉目录)。

### 2. 维度/周期映射(对齐 §5.5)

- `dimension` 直接取用户选中的 `dimensionFields` 成员,需满足平台校验 `^[a-zA-Z_][a-zA-Z0-9_]*$`(平台 `GovernanceIndicatorResource.drilldown` + `IndicatorDashboardService` 双重校验)。前端在构造请求前做同样的本地预校验,非法直接禁选。
- `period` 可选,按月粒度(平台 `DATE_TRUNC('month', dateColumn)`);当指标 `timeGrain` 非月或问句无明确周期时,**不传 `period`**(走全量下钻),不报错。
- **映射不上则降级**:维度为空 / `dimensionFields` 缺失时,不出下钻入口或回退到 dashboard 总览,并提示「该指标未配置可下钻维度」——绝不抛错或静默假装有数(对齐 D6)。

### 3. 取值与原地刷新

- 调 F1/F2 暴露的 BFF 下钻透传端点(后端 `PlatformIndicatorClient` 调平台 drilldown,机器账号鉴权 + 超时/降级)。
- 拿到 `List<{dimension, metric_value, row_count}>` 后,在 `src/components/copilot/useCopilotStream.ts` 的画布动作处理里,**用同 `artifactId` 调 sprint-27 F4 的 `upsertArtifact`**,把下钻结果替换为产物新 `spec`(echarts/DataTable 渲染下钻明细),保留 `id/sourceMessageId/title`,仅更新数据与可视化设置。
- 失败(超时/平台不可达)→ 显式提示「平台指标服务暂不可达」+「改用 AI 现生成」一键退回(复用 F4 降级路径),不静默。

## 影响范围

- `dts-copilot-webapp/src/types/artifact.ts`(`CanvasActionType` 增 `drilldown`)
- `dts-copilot-webapp/src/components/canvas/CanvasActions.tsx`(下钻动作 + 仅指标产物启用)
- `dts-copilot-webapp/src/components/canvas/CanvasActions.test.tsx`
- `dts-copilot-webapp/src/components/copilot/useCopilotStream.ts`(下钻 → 取值 → 原地 upsert)
- `dts-copilot-ai`:F1/F2 的 drilldown BFF 透传端点 + `PlatformIndicatorClient` 增 `drilldown(id, dimension, period)`

## 验证

- [ ] `pnpm test -- CanvasActions`:断言指标产物有「下钻」动作、非指标产物禁用。
- [ ] 单测:下钻结果通过同 `artifactId` upsert,产物 `id` 不变、数据更新。
- [ ] live contract:对平台 `GET /indicators/{id}/drilldown?dimension=X` 拿到 `{dimension, metric_value, row_count}` 行;证据记入 `it/`。
- [ ] 降级:维度缺失 / 平台超时时不报错,有显式提示与退回入口。

## 完成标准

- [ ] 指标产物可交互下钻,维度/周期来自 `dimensionFields/timeGrain`。
- [ ] 下钻在同 `artifactId` 原地刷新(复用 sprint-27 F4 upsert),不新增产物。
- [ ] 映射不上 / 取值失败时显式降级,不静默不报错。
- [ ] dts-platform 零改码(仅消费既有 drilldown 端点)。

## 证据

- `worklog/v1.0.0/sprint-29-202605/it/evidence/<date>-local/f5-t01-drilldown.md`
