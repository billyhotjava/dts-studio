# Sprint-26 F2/T03 signals 求值与 planner 预警分支验证

**时间**: 2026-05-30
**范围**: `OntologyService` signals 求值 / SQL plan 生成，以及 `AssetBackedPlannerPolicy` 预警查询分支。

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=OntologyServiceTest,AssetBackedPlannerPolicyTest test
```

结果：失败，planner 尚未声明预警查询 response kind。

关键失败：

```text
cannot find symbol
  symbol:   variable RISK_SIGNAL_QUERY
  location: class com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind
```

## GREEN 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=OntologyServiceTest,AssetBackedPlannerPolicyTest test
```

结果：

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖点：

- `OntologyService.buildSignalPlan("坏账风险")` 生成带 `GROUP BY` / `HAVING` 的只读 SQL plan。
- `OntologyService.evaluateSignals(...)` 覆盖命中/不命中阈值求值，返回 severity、advice、linkedActions。
- planner 对"哪些项目有坏账风险需要关注"返回 `RISK_SIGNAL_QUERY`。
- 预警分支使用 `L2_ONTOLOGY_SIGNAL`，source refs 指向 `public.xycyl_ads_flowerbiz_baddebt_summary`。
- prompt 包含 advice、linked actions 和预警 SQL，且发生在固定报表目录前。

## 可重跑脚本

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_signals_eval_and_planner.sh
```

预期输出：

```text
[static] signals evaluation and planner branch are present
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[junit] signals evaluation and planner regression tests passed
```
