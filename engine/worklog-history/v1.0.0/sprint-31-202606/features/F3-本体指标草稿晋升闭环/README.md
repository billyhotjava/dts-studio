# F3: 本体/指标草稿晋升闭环

**优先级**: P1
**状态**: DONE

## 目标

让离用户真实问题最近的 copilot 能**起草**语义定义（对象/指标/口径规则），草稿落治理层 draft、经人审晋升为正式定义后回流 pack——复制 sprint-26"草稿端点 + 人审、绝不直写"范式到语义治理，形成发现→定义→治理→消费的闭环。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | copilot 语义草稿端点 | P1 | DONE | F1-T02 |
| T02 | 草稿写入治理层 draft 状态 | P1 | DONE | T01,F2-T01 |
| T03 | 晋升审核流 + 回流 pack | P1 | DONE | T02 |

## 完成标准

- [x] copilot 可对"缺失对象/指标/口径规则"产出结构化草稿（不直改 SoT）
- [x] 指标草稿写入治理层 draft 状态，复用平台现有治理 DRAFT 承载面（2026-06-05 live platform 已验证）
- [x] 治理审通过 → 正式定义 → 经 F2 sync 回流 pack，闭环可跑通一例（2026-06-05 live：`it/evidence/20260605-local/semantic-draft-backflow-sync-complete.md`）
