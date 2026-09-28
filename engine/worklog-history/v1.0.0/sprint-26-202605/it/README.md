# Sprint-26 集成测试（IT）

本目录存放报花域本体化垂直切片的验收证据。**所有证据必须真实可重跑，不接受空占位。**

## 证据结构

```
it/
  README.md                  # 本文件，证据索引
  sql/                       # 回归问句集、对账 SQL、对象图导航样例
  evidence/<日期>-local/     # 每次本地验证的结果快照
```

## 证据矩阵

| Feature | 验收点 | 证据位置 | 状态 |
|---------|--------|---------|------|
| F0 | pack schema 向后兼容 | test_ontology_schema_and_load.sh + evidence/20260530-local/pack-schema-and-ontology-load.md | ✅ JUnit 3/3 绿 |
| F0 | OntologyService 加载单测 | test_ontology_schema_and_load.sh + evidence/20260530-local/pack-schema-and-ontology-load.md | ✅ JUnit 2/2 绿 |
| F0 | NL2SQL 回归基线 | sql/flowerbiz_baseline_questions.tsv + test_flowerbiz_nl2sql_baseline.sh + evidence/20260530-local/flowerbiz-nl2sql-baseline.md | ✅ JUnit 9/9 绿；邻近 planner/catalog 回归 38/38 绿 |
| F1 | 对象图 JOIN 生成 | test_object_graph_join.sh + evidence/20260530-local/object-graph-join.md | ✅ 静态校验通过；JUnit 8/8 绿 |
| F1 | planner 对象图导航分支 | test_object_graph_planner.sh + evidence/20260530-local/object-graph-planner.md | ✅ 静态校验通过；JUnit 26/26 绿 |
| F1 | 贯穿类 Golden Questions ≥90% | sql/flowerbiz_traversal_golden_questions.tsv + test_traversal_golden_questions.sh + evidence/20260530-local/traversal-golden.md | ✅ 静态校验通过；JUnit 1/1 绿；命中率 100% |
| F2 | metrics 集中口径定义 | test_flowerbiz_metrics.sh + evidence/20260530-local/flowerbiz-metrics.md | ✅ 静态校验通过；JUnit 4/4 绿 |
| F2 | signals 阈值预警定义 | test_flowerbiz_signals.sh + evidence/20260530-local/flowerbiz-signals.md | ✅ 静态校验通过；JUnit 5/5 绿 |
| F2 | signals 求值单测 | test_signals_eval_and_planner.sh + evidence/20260530-local/signals-eval.md | ✅ 静态校验通过；JUnit 25/25 绿 |
| F2 | 预警对账误差 <0.5% | test_signal_reconcile.sh + sql/signal_reconcile_prs_finance_cost.sql + evidence/20260530-local/signal-reconcile.md | ✅ DB 对账 0.0000%；坏账指标达标 |
| F3 | Action 定义与 endpoint 对齐 | test_flowerbiz_actions.sh + evidence/20260530-local/flowerbiz-actions.md | ✅ 静态校验通过；JUnit 6/6 绿 |
| F3 | Action 安全边界（只到草稿） | test_action_executor_safety.sh + evidence/20260530-local/action-safety.md | ✅ 静态校验通过；JUnit 9/9 绿 |
| F3 | 审批卡片 + guard + 审计 | test_action_guard_audit.sh + evidence/20260530-local/action-guard-audit.md | ✅ 静态校验通过；JUnit 12/12 绿 |
| F3 | 聊天 approve/cancel API 接线 | test_action_chat_approval_api.sh + evidence/20260530-local/action-chat-approval-api.md | ✅ 静态校验通过；JUnit 8/8 绿 |
| F3 | copilot-ai 运行态 adminapi 写回配置 | test_action_runtime_env_wiring.sh + evidence/20260530-local/action-runtime-live.md | ✅ compose env 可渲染；live approve 已创建 PRS 草稿 |
| F3 | 一键坏账草稿端到端审计链路 | evidence/20260530-local/baddebt-e2e-auth-blocker.md + evidence/20260530-local/action-runtime-live.md | ✅ guard 通过；`saveDraftFlowerBadDebt` 返回 200；审计日志 id=326；adminweb listPage 数据源返回草稿 |
| F4 | 本体化域接入 checklist | test_ontology_onboarding_checklist.sh + evidence/20260530-local/ontology-onboarding-checklist.md | ✅ 静态校验通过；项目域纸面演练已覆盖 |

## 重跑约定

沿用 sprint-25 风格：每个 `test_*.sh` 可独立执行，结果写入 `evidence/<日期>-local/`。标 DONE 前本矩阵所有"待产出"必须替换为真实证据链接。
