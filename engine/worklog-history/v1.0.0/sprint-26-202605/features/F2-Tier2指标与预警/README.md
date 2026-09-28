# F2: Tier2 指标与预警

**优先级**: P1
**状态**: DONE

## 目标

在对象图上叠加派生指标（metrics，集中定义口径）和阈值预警规则（signals，Palantir 的 kinetics）。系统主动识别风险/异常/机会并给出建议，让问答从"我问什么答什么"升级到"系统提醒我该关注什么"。仍为只读，不写回。

## 前置状态

F1 对象图已完成，T01 metrics、T02 signals、T03 求值/planner 分支与 T04 坏账预警对账均已完成。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | flowerbiz.json 补 metrics 定义 | P1 | DONE | F1/T01 |
| T02 | flowerbiz.json 补 signals 定义 | P1 | DONE | T01 |
| T03 | OntologyService signals 求值 + planner 预警分支 | P1 | DONE | T02, F1/T02 |
| T04 | 预警与 adminweb 固定报表对账 | P1 | DONE | T03 |

## 完成标准

- [x] metrics 口径与 dbt 4 列金额标准（rent/cost/sale/extra_cost）一致，集中定义、杜绝口径漂移。
- [x] signals 至少覆盖"坏账风险""欠费预警"两类，带 severity、advice、linkedActions。
- [x] planner 能识别"哪些有风险/异常/需关注"类问句并返回命中对象 + 建议。
- [x] 预警命中结果与 adminweb 对应固定报表对账误差 <0.5%，证据入 IT。
