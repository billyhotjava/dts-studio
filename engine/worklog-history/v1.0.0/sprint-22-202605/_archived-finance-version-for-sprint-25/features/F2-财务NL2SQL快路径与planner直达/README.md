# F2: 财务 NL2SQL 快路径与 planner 直达

**优先级**: P0
**状态**: READY

## 目标

让财务类问句尽量进入 `TEMPLATE_FAST_PATH` 或 `DIRECT_RESPONSE` 路径，减少进入 AGENT_WORKFLOW 兜底；当必须走 ReAct Agent 时，prompt 中携带的 finance 域 guardrails 与 fewShots 能稳定收敛 SQL。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 财务 NL2SQL 查询模板入库 | P0 | READY | F1-T02 |
| T02 | 财务域 planner 直达规则 | P0 | READY | F1-T02 |
| T03 | 财务域 few-shots 与 guardrails 沉淀 | P0 | READY | F1-T02, T01, T02 |

## 完成标准

- [ ] sprint README 中 8 条样本问句中 ≥ 6 条命中 TEMPLATE_FAST_PATH（时延 < 1s）
- [ ] 剩余问句进入 AGENT_WORKFLOW 时携带正确的 finance pack
- [ ] `Nl2SqlRoutingRule` 中 finance 域规则不与 procurement / project 冲突
- [ ] 失败回归集（不要走错链的反例）入文档
