# T03: 现有报花 NL2SQL 回归基线

**优先级**: P0
**状态**: DONE
**依赖**: 无

## 目标

在动任何运行时逻辑前，先固化报花域现有 NL2SQL 的回归基线，作为后续 F1/F2/F3 扩展"不退化"的对照线。

## 技术设计

- 收集 flowerbiz.json 现有 8 条 fewShots + 现网高频问句，整理成回归问句集。
- 记录每条问句当前命中的路由分支（BusinessDirectResponse / TemplateMatcher / BusinessObjectCatalog / AgentBiReportCatalog / free Workflow）与目标视图。
- 落地为可重跑脚本（参考 sprint-25 `it/test_*` 风格），输出 baseline 快照。

## 影响范围

- `it/sql/`、`it/` 下新增报花回归问句集与脚本。
- 不改任何生产代码。

## 验证

- [ ] 回归问句集 ≥ 现有 fewShots 数量，每条记录期望路由与目标视图。
- [ ] baseline 快照入 `it/evidence/`。

## 完成标准

- [x] 基线可重跑、结果稳定，作为 F1/F2/F3 完工时的对照。

## 2026-05-30 完成结论

产出 3 件：对照集 `it/sql/flowerbiz_baseline_questions.tsv`（8 fewShot + 2 护栏）、可跑回归网 `dts-copilot-ai/.../copilot/FlowerbizNl2SqlBaselineTest.java`、校验脚本 `it/test_flowerbiz_nl2sql_baseline.sh`。

- JUnit 路由基线 **9/9 绿**（离线，1.1s）：B01–B08 锁定模板快路径→对应 ADS；B10 锁定 L0 业务对象画像（F1 对象图导航不得抢占）。
- 静态一致性校验通过：期望 ADS 视图均存在于 flowerbiz.json。
- **⚠️ 预存红线**：现有 `AssetBackedPlannerPolicyTest` 主干已有 2 个失败（`trendQuestion...`→BUSINESS_ANALYSIS、`explicitNewReportRequest...`→reportCode null），非本任务引入。F2 接手前必须先三角定位「测试过时 vs 真实回归」。
- B09 口径歧义护栏写入 tsv，未硬断言，建议 F2 求值口径时硬化。

证据：`it/evidence/20260530-local/flowerbiz-nl2sql-baseline.md`。
