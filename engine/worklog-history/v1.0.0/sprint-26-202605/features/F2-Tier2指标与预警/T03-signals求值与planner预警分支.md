# T03: OntologyService signals 求值 + planner 预警分支

**优先级**: P1
**状态**: DONE
**依赖**: T02, F1/T02

## 目标

让 OntologyService 能把 signals 编译成可执行查询求值，并在 planner 新增"预警查询"分支，识别"哪些有风险/异常/需关注"类问句。

## 技术设计

- signals 求值：把 `when` 条件 + metrics 编译为 SQL，按 signal 绑定对象生成只读 SQL plan，返回命中对象列表。
- 返回结构：命中对象 + severity + advice + linkedActions（供 F3 挂动作卡片）。
- planner 分支：插在对象图导航分支附近，命中"风险/异常/预警/需关注/即将"等语义时触发。

## 影响范围

- `OntologyService` 新增 `buildSignalPlan` / `buildSignalPlans` / `evaluateSignals`。
- `AssetBackedPlannerPolicy` 新增 `RISK_SIGNAL_QUERY` 预警查询分支，输出 `L2_ONTOLOGY_SIGNAL`。

## 验证

- [x] 单测：构造命中/不命中数据，signals 求值结果正确。
- [x] "哪些项目有坏账风险"类问句路由到预警分支并返回 advice。
- [x] 回归基线无退化。
- [x] IT 脚本：`worklog/v1.0.0/sprint-26-202605/it/test_signals_eval_and_planner.sh`。
- [x] 证据：`worklog/v1.0.0/sprint-26-202605/it/evidence/20260530-local/signals-eval.md`。

## 完成标准

- [x] signals 求值 + planner 预警分支有单测覆盖。
