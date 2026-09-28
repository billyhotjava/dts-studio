# 报花域 NL2SQL 路由基线快照（F0/T03）

**日期**: 2026-05-30 本地
**环境**: Java 21 (GraalVM 21.0.10)，maven 离线（`~/.m2` 已就绪），dts-copilot-ai 模块

## 产出物

| 文件 | 作用 |
|------|------|
| `it/sql/flowerbiz_baseline_questions.tsv` | 对照集：8 条 fewShot + 2 条护栏问句，含期望路由分支与目标视图 |
| `dts-copilot-ai/.../copilot/FlowerbizNl2SqlBaselineTest.java` | 可跑的决策树回归网，锁定报花路由契约 |
| `it/test_flowerbiz_nl2sql_baseline.sh` | 静态一致性校验（视图存在性）+ 可选 `RUN_TEST=1` 跑 JUnit |

## 结果

### 1) JUnit 路由基线（新建）—— 全绿

```
mvn -o -pl dts-copilot-ai test -Dtest=FlowerbizNl2SqlBaselineTest
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.095 s
```

- B01–B08：8 条 fewShot 命中模板 → `TEMPLATE_FAST_PATH/TEMPLATE_SQL`，primaryTarget 指向对应 `public.xycyl_ads_flowerbiz_*`。
- B10：`报花单据状态分布` → `BUSINESS_INSIGHT` / `L0_BUSINESS_OBJECT_PROFILE` / `business-object:prs.flowerbiz.biz_order`。
  这是 F1 对象图导航**不得抢占**的分支，已锁定。

### 2) 静态一致性校验 —— 通过

```
[static] flowerbiz NL2SQL baseline consistency OK (10 rows)
```

每个期望 ADS 视图都存在于 `flowerbiz.json` objects，基线不会漂移到不存在的 mart。

## 3) Planner / Agent BI 邻近回归 —— 通过

本轮已修复此前记录的 Agent BI 目录路由漂移。重跑：

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackServiceTest,SemanticPackOntologySchemaTest,OntologyServiceTest,FlowerbizNl2SqlBaselineTest,AssetBackedPlannerPolicyTest,AgentBiReportCatalogServiceTest test
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
```

修正后的契约：已存在的 PRS 报花趋势资产优先命中 `FIXED_REPORT / L2_FIXED_REPORT`，与 `AgentBiReportCatalogServiceTest` 保持一致；不再把已有固定报表误降级为临时报表草稿或通用业务分析。

## 护栏说明：B09 口径歧义

B09 `本月收入多少`（租赁/销售/坏账/额外费用口径不同，未指定须澄清不得静默 SUM）已写入对照集 tsv，但**未在 JUnit 中硬断言**——因其落点依赖真实 catalog 在该问句上的匹配结果，需运行态确认。现阶段由 `flowerbiz.json` guardrails + 现有通用用例 `ambiguousBusinessQuestionUsesAgentWorkflowInsteadOfHardClarification` 覆盖；建议在 F2 求值口径时硬化为显式断言。

## 复现

```bash
cd dts-copilot
# 静态
worklog/v1.0.0/sprint-26-202605/it/test_flowerbiz_nl2sql_baseline.sh
# 静态 + JUnit
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_flowerbiz_nl2sql_baseline.sh
# 邻近回归
mvn -pl dts-copilot-ai -Dtest=SemanticPackServiceTest,SemanticPackOntologySchemaTest,OntologyServiceTest,FlowerbizNl2SqlBaselineTest,AssetBackedPlannerPolicyTest,AgentBiReportCatalogServiceTest test
```
