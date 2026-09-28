# Sprint-26 F3/T02 Action 编排与草稿调用安全验证

**时间**: 2026-05-30
**范围**: `OntologyActionExecutor` action 编排、payload 组装、adminapi 草稿调用安全边界。

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=OntologyActionExecutorTest test
```

结果：失败，执行层接口和实现不存在。

关键失败：

```text
cannot find symbol
  symbol:   class AdminApiActionClient
```

## GREEN 证据

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_action_executor_safety.sh
```

结果：

```text
[static] action executor is locked to draft endpoint
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[junit] action executor safety tests passed
```

覆盖点：

- 根据 action.params 从对象属性组装 `projectId`、`draftItemJson`、`badDebtType`。
- 只调用 `endpoint.draft` / `postDraft`。
- 测试断言 `postCommit` never，执行器源码静态检查不允许出现 `postCommit` 调用。
- required param 缺失时在调用 adminapi 前拒绝。
- adminapi 返回错误时透传错误消息，不静默吞错。
