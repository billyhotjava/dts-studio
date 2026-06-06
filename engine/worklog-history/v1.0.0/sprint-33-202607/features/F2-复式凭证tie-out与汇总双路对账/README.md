# F2: 复式凭证 tie-out 与汇总双路对账

**优先级**: P0
**状态**: IN_PROGRESS

## 目标

让 copilot 的财务汇总数据可被证明：一是双路独立计算对账（copilot vs oracle 报表/黄金 SQL），二是锚到已过账凭证的复式（借=贷）账本——后者是会计级证明，不是"看起来对"。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 凭证账本接入（借=贷 复式作为汇总锚） | P0 | DONE | F1-T01 |
| T02 | 汇总双路计算对账 | P0 | IN_PROGRESS | F1-T02 |
| T03 | 收入/应收/回款锚到凭证科目 | P0 | IN_PROGRESS | T01,T02 |

## 完成标准

- [ ] 凭证账本（debit/credit + subjectId）可作为对账锚接入
- [ ] 核心汇总 copilot SUM 与 oracle 端点/黄金 SQL 双路相等（到分）
- [x] 月对账、售账、2026 凭证三核心统计已建立 ADS vs 应用 MySQL 原表只读 SQL 证明链路，避免用 ODS 或 Trino MySQL catalog 绕路验证
- [x] 本地 subject tie-out contract 可将核心汇总锚到凭证科目借/贷侧，坏账独立映射到损失科目
- [ ] 收入/应收/回款聚合能 tie-out 到真实凭证科目，借贷平衡、差异为 0 或登记
