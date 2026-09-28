# F5: 范式固化与 IT 证据

**优先级**: P2
**状态**: DONE

## 目标

把财务垂直切片跑通的经验沉淀为"空白域建模 onboarding checklist"(供库存/督导/薪资域复用),并汇总 Sprint-30 的真实集成测试证据。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 空白域建模 onboarding checklist | P2 | DONE | F4 |
| T02 | Sprint-30 IT 证据包 | P2 | DONE | F1-F4 |

## 完成标准

- [x] checklist 可指导下一个空白域(库存)从 ODS→dbt→语义包→对账走完
- [x] IT 证据矩阵全部为真实可重跑证据,无空占位

## 证据

- `assets/blank-domain-onboarding-checklist.md`
- `it/test_f5_evidence_pack.sh`
- `it/evidence/20260601-local/f5-sprint30-evidence-pack.md`
