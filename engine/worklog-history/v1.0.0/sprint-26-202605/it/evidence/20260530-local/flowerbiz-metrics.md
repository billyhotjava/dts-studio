# Sprint-26 F2/T01 metrics 定义验证

**时间**: 2026-05-30
**范围**: `flowerbiz.json` 的 Tier2 metrics 集中口径定义。

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest test
```

结果：失败，`flowerbiz.metrics()` 仍为空。

关键失败：

```text
Expecting actual:
  []
to contain exactly:
  ["租金净额", "处理成本", "销售金额", "额外费用", "坏账租金损失", "项目坏账率", "客户在租金额"]
```

## GREEN 证据

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_flowerbiz_metrics.sh
```

结果：

```text
[static] flowerbiz tier2 metrics are present
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[junit] flowerbiz tier2 metric schema tests passed
```

覆盖点：

- metrics 集中定义 7 个指标：租金净额、处理成本、销售金额、额外费用、坏账租金损失、项目坏账率、客户在租金额。
- caliber 覆盖 dbt 4 列金额标准：`dbt_amount:rent`、`dbt_amount:cost`、`dbt_amount:sale`、`dbt_amount:extra_cost`。
- `expr` 引用的字段均存在于对应对象的 `keyDimensions` / `keyMeasures` / `commonFilters` / `defaultTimeField`。
- 指标格式覆盖 `currency` 与 `percent`。
