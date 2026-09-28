# F8: 后台契约与降级联调

**优先级**: P0
**状态**: DONE

## 目标

把 Sprint-27 中 F5/F6 依赖的 agent 后台字段从“文档提醒”变成可执行的契约轨：明确同步响应、SSE `done`、会话恢复、前端降级、mock 验收之间的边界，避免前端 UI 做完后因为后台未返回 `assumptions` / `trace` 等字段而无法验收。

## 背景

当前 `AiAgentChatMessage` / `CopilotStreamEvent.done` 只有 `generatedSql`、`routedDomain`、`targetView`、`dataSurface`、`sourceRefs` 等字段；F5 的口径芯片、低置信反问、改口径重算，以及 F6 的结构化溯源都需要新增契约。Sprint 主线允许前端“有则用、无则降级”，但完整验收必须有后台契约支撑。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 聊天响应契约扩展 | P0 | DONE | F3-T00 |
| T02 | 重算与澄清入参契约 | P0 | DONE | T01 |
| T03 | 溯源与纠正契约桩 | P0 | DONE | T01 |
| T04 | 契约 mock 与降级验收 | P0 | DONE | T01,T02,T03 |

## 完成标准

- [x] 同步响应、SSE `done`、会话恢复三条链路都能透传 `assumptions` / `confidence` / `clarifications` / `trace`。
- [x] 发送链路能透传并消费 `assumptionOverrides` / `clarificationAnswers`，字段缺失或旧后台场景前端有明确降级，不误标全功能完成。
- [x] `submitCaliberCorrection` 本期作为 feedback-first 桩存在，P2 再接真正评测集/本体草稿写回。
- [x] F5/F6 的 IT 验收区分“mock 契约通过”和“真实后台契约通过”，证据分别落到 `it/README.md`。

## 实施摘要

- AI 服务新增 `CopilotChatContract`,由 `ConversationPlan` + SQL 构造口径假设、置信度和 trace,并写入 `AiChatMessage` JSONB 字段。
- `AgentExecutionService` 的 SSE `done`、`AgentChatResource` / `InternalAgentChatResource` 的同步与会话恢复响应均输出同一契约字段。
- analytics `CopilotChatResource` / `CopilotAgentChatClient` 将 `assumptionOverrides` / `clarificationAnswers` 转发到 AI 内部服务。

## 验证证据

- `it/evidence/20260531-local/f8-backend-contract.md`
