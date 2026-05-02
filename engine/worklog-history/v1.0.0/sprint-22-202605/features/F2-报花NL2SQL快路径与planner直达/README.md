# F2: 报花 NL2SQL 快路径与 planner 直达

**优先级**: P0
**状态**: READY
**轨**: 1（dts-copilot 智能层）

## 目标

让报花类问句尽量进入 `TEMPLATE_FAST_PATH`，减少进入 AGENT_WORKFLOW 兜底。当必须走 ReAct Agent 时，prompt 携带 flowerbiz pack 的 guardrails 与 fewShots 收敛 SQL。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 报花 NL2SQL 查询模板入库 | P0 | READY | F1-T02 |
| T02 | 报花域 planner 直达规则 | P0 | READY | F1-T02 |
| T03 | 报花 few-shots 与 guardrails 沉淀 | P0 | READY | F1-T02, T01, T02 |

## 完成标准

- [ ] 12+ 条 flowerbiz query template 入库（按 8 张 ads + 高频组合）
- [ ] sprint README 中 8 类样本问句中 ≥ 6 条命中 TEMPLATE_FAST_PATH（< 1s）
- [ ] `Nl2SqlRoutingRule` 中 flowerbiz 域权重 ≥ procurement / project，避免被错路由
- [ ] 失败回归集（不要走错链的反例）入文档
