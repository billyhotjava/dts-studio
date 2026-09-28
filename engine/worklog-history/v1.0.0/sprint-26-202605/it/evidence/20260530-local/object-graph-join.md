# Sprint-26 F1 对象图 JOIN 生成验证

**时间**: 2026-05-30
**范围**: F1/T01 `flowerbiz.json` links 定义；F1/T02 `OntologyService` 对象图路径与 JOIN SQL 生成。

## RED 证据

### T01 links 定义

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest test
```

结果：失败，`flowerbiz.links()` 仍为空。

关键失败：

```text
Expecting actual:
  []
to contain exactly:
  ["客户_项目", "项目_报花", "报花_采购", "报花_结算"]
```

### T02 JOIN 生成 API

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=OntologyServiceTest test
```

结果：失败，缺少 `OntologyService.JoinPlan`、`OntologyModel.buildJoinPlan(...)`、`buildJoinPlans(...)`。

## GREEN 证据

### 静态 pack 校验

命令：

```bash
worklog/v1.0.0/sprint-26-202605/it/test_object_graph_join.sh
```

结果：

```text
[static] flowerbiz object graph links are present
```

### JUnit 回归

命令：

```bash
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_object_graph_join.sh
```

结果：

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[junit] ontology object graph JOIN tests passed
```

覆盖点：

- `flowerbiz.json` 加载 4 条 Tier1 links：`客户_项目`、`项目_报花`、`报花_采购`、`报花_结算`。
- links 关联对象存在：`客户`、`项目`、`租赁报花明细`、`采购明细`、`结算单`。
- 客户→项目→报花生成两跳 `LEFT JOIN`，保留孤儿行。
- 报花→结算通过 `jsonb_array_elements_text(o1."biz_ids_json"::jsonb)` 展开匹配。
- 多条最短路径返回候选 `JoinPlan`，不随意选择。
