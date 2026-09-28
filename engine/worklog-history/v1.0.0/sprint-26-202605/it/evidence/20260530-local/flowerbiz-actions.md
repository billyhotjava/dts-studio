# Sprint-26 F3/T01 actions 定义验证

**时间**: 2026-05-30
**范围**: `flowerbiz.json` 的 Tier3 `actions` 定义，以及坏账草稿 adminapi/adminweb endpoint 对齐。

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest test
```

结果：失败，`flowerbiz.actions()` 里没有 `创建坏账处理单`。

关键失败：

```text
SemanticPackOntologySchemaTest.shouldLoadFlowerbizPackWithTier3Actions » NoSuchElement No value present
```

## GREEN 证据

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_flowerbiz_actions.sh
```

结果：

```text
[static] flowerbiz tier3 bad-debt action is present and endpoint-aligned
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[junit] flowerbiz tier3 action schema tests passed
```

覆盖点：

- `创建坏账处理单` action 绑定 `租赁报花明细` 对象。
- draft endpoint 对齐 `/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt`。
- commit endpoint 仅声明 `/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt`，后续执行层不得自动调用。
- params 覆盖 `projectId`、`draftItemJson`、`badDebtType`，source 均能解析到本体对象字段。
- `approval=human`、`audit=true`、`guard=flowerbiz:baddebt:draft`。
- `坏账风险.linkedActions` 能闭合到该 action。
