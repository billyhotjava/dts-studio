# T03: 审批卡片 + 权限 guard + 审计日志

**优先级**: P1
**状态**: DONE
**依赖**: T02

## 目标

在 copilot 回复中挂"建议动作"卡片，执行前做权限 guard，执行后写审计日志，保证人在环路、全程可追溯。

## 技术设计

- **建议动作卡片**：当问答结论关联 signal.linkedActions 时，回复挂卡片（动作名 + 目标对象 + 将创建草稿的说明），用户点击确认才发起。
- **权限 guard**：执行前校验当前用户是否具备 action.guard 声明的权限（复用现有鉴权上下文）；不足则拒绝并说明。
- **审计日志**：记录 who/when/action/object/params/草稿单号/结果，落审计表或日志通道。

## 影响范围

- copilot 回复渲染层（动作卡片结构）。
- 鉴权集成、审计写入。

## 验证

- [x] 无权限用户被拒绝并得到清晰原因。
- [x] 每次草稿创建都有完整审计记录。
- [x] 用户未确认时不发起任何写回。
- [x] 聊天层 `/approve` / `/cancel` 接入审批服务，并返回前端可消费的聊天响应结构。

## 完成标准

- [x] guard / 审计 / 人工确认三道关卡均有用例覆盖。

## 验收证据

- `it/test_action_guard_audit.sh`
- `it/evidence/20260530-local/action-guard-audit.md`
- `it/test_action_chat_approval_api.sh`
- `it/evidence/20260530-local/action-chat-approval-api.md`

## 落地说明

- 新增 `OntologyActionApprovalService`：统一生成 action approval card，并在 `confirmed=false` 时只返回卡片、不触发 `createDraft`。
- 新增 `ActionGuardService`：复用 `CopilotUserContext.roles()` 校验 action.guard，缺少 `flowerbiz:baddebt:draft` 时拒绝。
- 扩展 `AiAuditService.logActionExecution`：记录 `userId/sessionId/actionName/objectName/guard/input/result/success/errorMessage`。
- 扩展 `AgentChatResource`：`/approve` 解析 `domain:actionName` 并携带当前用户上下文调用 `requestDraft`；`/cancel` 返回取消态聊天消息。
