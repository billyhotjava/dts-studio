# Sprint-26 F3/T03-T04 聊天审批 API 接线证据

**时间**: 2026-05-30
**范围**: Agent Chat approve/cancel API、审批表单参数透传、adminapi base URL fail-fast。

## 变更点

- `AgentChatResource` 新增 `/api/ai/agent/chat/approve` 与 `/api/ai/agent/chat/cancel`。
- `/approve` 将 `actionId=<domain>:<actionName>`、`formData`、当前 `CopilotUserContext` 转交 `OntologyActionApprovalService.requestDraft`。
- 返回体保持前端聊天响应结构：`agentMessage`、`response`、`toolCalls`、`requiresApproval`、`pendingAction`。
- `OntologyActionExecutor` 支持审批表单按 action param 名直接提交：`projectId`、`draftItemJson`、`badDebtType`。
- `HttpAdminApiActionClient` 缺少 `copilot.action.adminapi.base-url` 时 fail-fast，避免误打 `dts-copilot-proxy`。

## 验证命令

```bash
RUN_TEST=1 bash worklog/v1.0.0/sprint-26-202605/it/test_action_chat_approval_api.sh
```

## 结果

```text
[static] chat approve/cancel API, approved form params, and adminapi base-url guard are wired
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[junit] chat approval API tests passed
```

覆盖用例：

- `AgentChatActionApprovalResourceTest` 2/2 绿。
- `OntologyActionExecutorTest` 4/4 绿。
- `HttpAdminApiActionClientTest` 2/2 绿。

## 补充覆盖

- 真实 PRS adminapi `saveDraftFlowerBadDebt` 已在 `baddebt-e2e-auth-blocker.md` 中通过 direct gateway + 业务 Authorization 验证。
- 已在 `action-runtime-live.md` 中补齐正在运行的 `dts-copilot-ai` 容器链路：聊天 approve -> PRS adminapi draft -> Copilot 审计日志。
