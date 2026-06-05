# T04: 漂移台账与收口决策（ADR）

**优先级**: P1
**状态**: READY
**依赖**: T01, T02

## 目标

把 T01 的差异、T02 的定源决策固化为一份架构决策记录（ADR），明确"每条口径以谁为准、为什么、收口后谁消费谁"，作为不可随意回退的团队契约。

## 技术设计

ADR 内容（`assets/ADR-001-caliber-single-source-of-truth.md`）：
1. **决策**：治理层（dts-platform modeling + OpenMetadata glossary）为口径 SoT；pack 为 agent 投影 + 草稿面；dbt mart 按 SoT 物化。
2. **落点分工**：对象/属性/指标定义 → modeling；术语/同义词 → glossary。
3. **每条漂移项裁定表**：引用 T01 矩阵，逐条给"以谁为准 + 处置动作（改 pack / 改 dbt / 改 glossary）"。
4. **后果与约束**：pack guardrails 不再手维护（F2）；新增/变更口径走草稿晋升（F3）；违反即回归红（F4）。
5. **已知风险与不做项**：不抽 modeling 微服务；本轮只收 finance/procurement 两域。

## 影响范围

- 产出 `assets/ADR-001-caliber-single-source-of-truth.md`
- 可能触发少量"立即对齐"动作（明显错误的一源直接改正，记录在 ADR 处置表）

## 验证

- [ ] ADR 经技术负责人评审签署
- [ ] 每条漂移项都有明确处置动作与责任源

## 完成标准

- [ ] ADR 成文并作为 F2/F3/F4 的依据；漂移裁定表 100% 覆盖 T01 矩阵
