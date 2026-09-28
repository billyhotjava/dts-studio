# Sprint-26 F2/T02 signals 定义验证

**时间**: 2026-05-30
**范围**: `flowerbiz.json` 的 Tier2 signals 阈值预警定义。

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest test
```

结果：失败，`flowerbiz.signals()` 仍为空。

关键失败：

```text
Expecting actual:
  []
to contain exactly:
  ["坏账风险", "欠费预警"]
```

## GREEN 证据

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_flowerbiz_signals.sh
```

结果：

```text
[static] flowerbiz tier2 signals are present
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[junit] flowerbiz tier2 signal schema tests passed
```

覆盖点：

- signals 定义 `坏账风险`、`欠费预警` 两类。
- severity 分别为 `high`、`medium`。
- `when` 条件至少引用已定义 metric。
- `坏账风险` 带 `linkedActions=["创建坏账处理单"]`，等待 F3 actions 闭合校验。
- 每个 signal 绑定对象均存在，advice 非空。
