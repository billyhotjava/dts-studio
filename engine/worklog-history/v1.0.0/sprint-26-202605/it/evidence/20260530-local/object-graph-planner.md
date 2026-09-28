# Sprint-26 F1 对象图 planner 导航验证

**时间**: 2026-05-30
**范围**: F1/T03 `AssetBackedPlannerPolicy` 对象图导航决策分支。

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest test
```

结果：失败，新 planner 契约尚不存在。

关键失败：

```text
cannot find symbol
  symbol:   variable OBJECT_GRAPH_NAVIGATION
  location: class com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind
```

## GREEN 证据

### planner 单测

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest test
```

结果：

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
```

### 可重跑 IT 脚本

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_object_graph_planner.sh
```

结果：

```text
[static] object graph planner branch is present
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[junit] object graph planner regression tests passed
```

覆盖点：

- `从客户到项目再到租赁报花明细的全链路追溯` 路由到 `OBJECT_GRAPH_NAVIGATION`。
- plan 携带 `dataSurface=L1_ONTOLOGY_GRAPH`、`primaryTarget=ontology:flowerbiz`、JOIN sourceRefs。
- prompt 包含对象图导航说明和 `LEFT JOIN` 链路 SQL。
- `报花单据状态分布` 仍走 L0 业务对象画像，F1 分支不误抢单对象问题。
- F0/T03 报花 NL2SQL 基线 9/9 绿。
