# Sprint-26 F3/T03 Action 审批卡片、权限 guard 与审计验证

**时间**: 2026-05-30
**范围**: `OntologyActionApprovalService`、`ActionGuardService`、`AiAuditService.logActionExecution`。

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=OntologyActionApprovalServiceTest test
```

结果：失败，approval service 与 action audit 事件尚不存在。

关键失败：

```text
cannot find symbol
  symbol:   class OntologyActionApprovalService
```

## GREEN 证据

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_action_guard_audit.sh
```

结果：

```text
[static] action approval guard and audit chain is wired
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[junit] action guard/audit tests passed
```

覆盖点：

- 用户未确认时仅返回审批卡片结构，`createDraft` never。
- 审批卡片包含 `actionId/toolId/params/microForm`，可被现有前端 pendingAction 卡片消费。
- 当前用户缺少 `flowerbiz:baddebt:draft` guard 时拒绝，并写失败审计。
- 用户确认且 guard 通过后才调用 `OntologyActionExecutor.createDraft`。
- 审计事件记录 `userId/sessionId/actionName/objectName/guard/input/result/success/errorMessage`。
