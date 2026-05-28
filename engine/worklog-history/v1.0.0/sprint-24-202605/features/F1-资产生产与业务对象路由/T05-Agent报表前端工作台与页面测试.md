# T05: Agent 报表前端工作台与页面测试

**优先级**: P0  
**状态**: DONE  
**依赖**: T04

## 目标

完善 Copilot 前端 `AI 报表` 页面，让 Sprint-24 的“报表资产生产器 + 业务对象问答器”在界面上有明确入口，而不是只展示经营报表快捷问题。

## 技术设计

- 页面顶部展示 Agent BI 工作流：L2 固定报表、L1 dbt 主题表、L0 业务对象、ACT 动作提案。
- 报表资产生产器保留已有经营问题入口，用于触发 `REPORT_DRAFT`。
- 业务对象问答器新增表格化入口，覆盖：
  - 报花单据状态分布
  - 采购配送记录状态
  - 项目点状态统计
  - 银行流水未核对
  - 库存和出入库画像
- 业务对象按钮通过 Copilot prompt request 事件投递，`source=business-object`，保持只读问答边界。

## 影响范围

- `dts-copilot-webapp/src/pages/AgentReportsPage.tsx`
- `dts-copilot-webapp/src/pages/AgentReportsPage.css`
- `dts-copilot-webapp/src/pages/AgentReportsPage.test.tsx`
- `dts-copilot-webapp/src/pages/agent-reports/agentReportQuickStarts.ts`
- `dts-copilot-webapp/src/pages/agent-reports/agentReportPromptHandoff.ts`
- `dts-copilot-webapp/src/pages/agent-reports/agentReportQuickStarts.test.ts`
- `dts-copilot-webapp/src/i18n.ts`

## 验证

- [x] `pnpm vitest run src/pages/agent-reports/agentReportQuickStarts.test.ts src/pages/AgentReportsPage.test.tsx`
- [x] `pnpm typecheck`
- [ ] `pnpm test` 全量前端测试仍有既有 React 19/Testing Library 异步渲染类失败，非本任务新增页面失败。

## 完成标准

- [x] 页面明确区分“报表资产生产器”和“业务对象问答器”。
- [x] 业务对象入口覆盖报花、采购、项目、财务、仓库首批对象。
- [x] 页面测试覆盖对象列表渲染和 Copilot 事件投递。
