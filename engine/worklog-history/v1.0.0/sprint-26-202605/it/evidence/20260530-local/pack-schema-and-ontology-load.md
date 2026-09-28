# Pack Schema 与 OntologyService 加载证据（F0/T01-T02）

**日期**: 2026-05-30 本地
**环境**: dts-copilot-ai，Java 21，Maven

## 覆盖范围

- `links` / `metrics` / `signals` / `actions` 四个 semantic-pack 可选节已建模。
- 未声明四节的旧 pack 返回空列表，不抛异常。
- `flowerbiz.json` 显式声明四个空数组，后续 F1/F2/F3 再填业务内容。
- 四节中缺必填字段的非法条目会 WARN 并跳过，其余合法条目继续加载。
- `OntologyService` 可从 typed semantic pack 构建 objectIndex、linkGraph、metricIndex、signalIndex、actionIndex。
- `OntologyService` 仅提供只读加载/索引，未接入 planner 决策树。

## TDD 记录

红灯：

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest,OntologyServiceTest test
```

首次运行失败在测试编译期，缺少 `SemanticPackService.SemanticObject` 等 typed schema 模型与 `OntologyService`，符合 F0/T01-T02 的未实现缺口。

绿灯：

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest,OntologyServiceTest test
```

结果：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。

## 邻近回归

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackServiceTest,SemanticPackOntologySchemaTest,OntologyServiceTest,FlowerbizNl2SqlBaselineTest,AssetBackedPlannerPolicyTest,AgentBiReportCatalogServiceTest test
```

结果：`Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`。

本轮同时修复了基线证据中记录的 2 个 Agent BI 目录路由漂移：

- `trendQuestionUsesAgentBiCatalogReportDraftEvenWithoutGenerateVerb`
- `explicitNewReportRequestUsesAgentGeneratedReportDraftInsteadOfFixedReportCandidates`

修正后的契约与 `AgentBiReportCatalogServiceTest` 保持一致：已存在的 PRS 报花趋势资产优先命中 `FIXED_REPORT / L2_FIXED_REPORT`，不再降级为临时报表草稿。

## 重跑

```bash
cd dts-copilot
worklog/v1.0.0/sprint-26-202605/it/test_ontology_schema_and_load.sh
RUN_TEST=1 worklog/v1.0.0/sprint-26-202605/it/test_ontology_schema_and_load.sh
```
