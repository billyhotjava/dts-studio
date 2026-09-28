# Sprint-26 F1 贯穿类 Golden Questions 回归验证

**时间**: 2026-05-30
**范围**: F1/T04 报花域对象图导航 Golden Questions，验证命中率、JOIN 路径、sourceRefs、dataSurface、孤儿提示。

## Golden 集

文件：

```text
it/sql/flowerbiz_traversal_golden_questions.tsv
```

覆盖 4 条对象图链路问题：

- G01 客户 → 项目 → 报花 → 结算
- G02 项目 → 报花 → 采购
- G03 客户 → 报花 → 结算缺口
- G04 报花 → 结算

## RED 证据

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=FlowerbizTraversalGoldenQuestionTest test
```

结果：失败，命中率为 0.0，低于 0.9。

关键失败：

```text
Expecting actual:
  0.0
to be greater than or equal to:
  0.9
```

失败原因：

- G01/G04 已进入对象图导航，但缺少显式 `link path`，无法证明 JOIN 路径正确。
- G02 `对应采购单` 未被识别为对象图关系语义，被业务对象画像抢占。
- G03 `还没结算` 未被识别为对象图关系语义，被固定报表目录抢占。

## GREEN 证据

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_traversal_golden_questions.sh
```

结果：

```text
[static] flowerbiz traversal golden questions are present
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[junit] traversal golden question regression passed
```

覆盖点：

- Golden Questions 4/4 命中 `OBJECT_GRAPH_NAVIGATION`，命中率 100%，满足 ≥90%。
- plan 保留 `dataSurface=L1_ONTOLOGY_GRAPH`。
- plan 保留预期 `sourceRefs`。
- prompt 保留 `link path`，例如 `客户_项目 -> 项目_报花 -> 报花_结算`。
- 需要孤儿提示的问题保留 `可能孤儿` 或通用 LEFT JOIN 孤儿提示。
