# F4: 路由结果接入 agent-first UI(前端 dts-copilot-webapp)

**优先级**: P1
**阶段**: P2
**状态**: DONE

## 目标

把 F3(后端指标优先路由)的**指标命中结果**在 copilot 的对话脊柱 + 活产物画布里呈现:命中指标渲染为 Indicator 产物(复用 F2)、命中指标名做成可改芯片(切候选 / 退回 AI 现生成)、溯源做**信任分层**(权威平台指标 vs AI 现生成)。全程**复用 sprint-27 F5/F6** 的芯片交互、乐观执行重算通道与 `TracePanel`,**不新造**第二套芯片/重算/溯源机制。

## 背景

F3 在后端命中已发布指标后,按 sprint-27 F8 契约把指标元数据填进 `trace.metricCaliber`(name/formula/version/ontologyRef)、把命中指标做成 `assumptions` 里的可改芯片、把权威取值数据放进产物;未命中则退回现生成 SQL。前端需要让这套响应在 UI 上**一眼可分**「权威指标」与「AI 现算」,并让用户能就地切候选指标或退回生成 SQL——这正是 sprint-27 F5(口径芯片/乐观执行)与 F6(溯源)已建好的交互骨架的延伸,本 Feature 只做接入与信任分层呈现。

## 底座依赖

- **F2**(本 Sprint):`Artifact` 的 `type: 'indicator'` 产物类型与渲染底座、指标 API 客户端。
- **F3**(本 Sprint):后端路由命中结果(指标命中产物数据 + `trace.metricCaliber` + 命中指标可改芯片 `assumptions`)。
- **sprint-27 F5**:`assumptions/EditableAssumptionChips.tsx`、`ConversationThread.handleAssumptionCommit` → `handleSendText`(`assumptionOverrides` / `replaceAssistantMessageId` / `recomputeArtifactId`)重算通道、`assumptionConfidence.ts`(`OPTIMISTIC_CONFIDENCE_THRESHOLD=0.6`)。
- **sprint-27 F6**:`TracePanel.tsx`(口径 / 来源 / SQL 分区,`buildTraceModel` / `formatCaliber`,`trace.metricCaliber`)。
- **sprint-27 F8**:`api/types.ts` 的 `CopilotAssumption` / `CopilotTrace` / `CopilotTraceMetricCaliber` 契约(零新增形状,仅取值)。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 指标命中产物入对话/单窗口结果面 | P1 | DONE | F2, F3 |
| T02 | 指标可改芯片(复用 F5) | P1 | DONE | T01, sprint-27 F5 |
| T03 | 权威溯源信任分层(复用 F6) | P1 | DONE | T01, sprint-27 F6 |

## 当前进展(2026-05-31)

- T01 已完成单窗口结果面:若助手消息为 `PUBLISHED_INDICATOR`, `InlineIndicatorPreview` 在消息内调用 analytics BFF 取 `detail` 并渲染 `type:'indicator'` artifact;不恢复右侧预览窗。
- T02 已复用 sprint-27 F5 assumptions 通道:后端返回 `metric` 可改芯片,候选指标和「退回 AI 现生成」均走既有重算通道。
- T03 已复用 sprint-27 F6 trace:平台指标徽标/口径/版本均取 `trace.metricCaliber`,退回现生成则不显示平台指标徽标。

## 范围边界

- 只做**呈现与交互合流**:消费 F3 的响应字段,不在前端重算口径(数值恒取平台)、不新增契约形状、不新造芯片/重算/溯源组件。
- 切候选指标 / 退回 AI 现生成,统一走 sprint-27 F5 已有的 `handleAssumptionCommit` → `handleSendText` 重算通道,由后端 F3 重新路由;前端不伪造取值。
- `drilldown` 交互式下钻、口径 version 变更提醒、多候选下拉的丰富 UI 属 **F5**(P3);本 Feature 仅保证命中→产物→芯片→溯源的基础合流闭环。

## 完成标准

- [x] F3 命中指标时,响应里的指标取值渲染为 `type:'indicator'` 产物进单窗口结果面(复用 F2),对话内该消息标注「来自平台指标」标识。
- [x] 命中指标名渲染为「指标=XX」可改芯片;可切其他候选指标或选「退回 AI 现生成」,提交后走 sprint-27 F5 既有重算通道触发后端重新路由。
- [x] `TracePanel`/消息徽标区分平台指标口径与现生成 SQL;口径芯片取值与溯源同源(同取 `trace.metricCaliber`)。
- [x] 平台不可达 / 路由降级时,UI 显式回落到错误提示或现生成呈现,不静默假装权威。
- [x] 单测覆盖:命中标识、inline indicator preview、芯片候选/退回契约。
- [x] dts-platform 指标业务零改码;跨服务认证只读白名单由 F1 统一收口。
