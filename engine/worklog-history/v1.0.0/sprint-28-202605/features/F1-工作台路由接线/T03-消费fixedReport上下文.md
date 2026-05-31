# T03: 消费 `fixedReport` / `fixedReportTemplate` 上下文

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

从看板、大屏或固定报表入口跳到 `/agent-bi?fixedReport=xxx` 后,工作台保留模板上下文并触发对应执行或展示明确错误态。

## 技术设计

- 在 `AgentWorkspacePage` 读取 `fixedReport` 和 `fixedReportTemplate` 两类 query。
- 复用 `dts-copilot-webapp/src/pages/fixed-reports/fixedReportSurfaceEntry.ts` 的 query key 约定。
- 将模板编码转成 `CopilotPromptRequest`,交给现有 `ConversationThread` 自动执行。
- 模板编码为空或不存在时展示受控错误态,不进入空白冷启动。

## 影响范围

- `dts-copilot-webapp/src/pages/AgentWorkspacePage.tsx`
- `dts-copilot-webapp/src/pages/fixed-reports/fixedReportSurfaceEntry.ts`
- `dts-copilot-webapp/src/components/copilot/copilotPromptRequest.ts`
- `dts-copilot-webapp/src/pages/AgentWorkspacePage.test.ts`

## 验证

- [x] `pnpm test -- AgentWorkspacePage`
- [x] Playwright 打开 `/agent-bi?fixedReport=PRS-FLOWERBIZ-OVERVIEW`,确认对话脊柱开始执行固定报表请求或展示明确的模板错误态。

## 完成标准

- [x] 固定报表 query 不丢失。
- [x] 成功路径进入对话/画布工作区。
- [x] 错误路径可见、可返回,不白屏。

## 证据

- `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f1-workspace-routing.md`
