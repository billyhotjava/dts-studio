# T01: 语义收口 onboarding 文档

**优先级**: P2
**状态**: READY
**依赖**: F1-F4

## 目标

把本 sprint 跑通的语义收口流程抽象成可复用清单，让后续域/场景照单收口，降低每域成本（呼应 sprint-30 F5 的空白域建模 checklist，本份聚焦"口径治理"维度）。

## 技术设计

清单覆盖（从本 sprint 实践提炼）：
1. 三源差异盘点（对照 9 铁律）
2. 定源 ADR（每条以谁为准）
3. 口径铁律 → 机器可检规则
4. 治理层导出契约 → pack 生成（手维护下线）
5. 漂移检测 + 降级
6. 草稿晋升闭环接线
7. 跨源回归入 CI

形式：`assets/semantic-consolidation-onboarding.md`，带 finance 域填好的样例列。

## 影响范围

- 产出 `assets/semantic-consolidation-onboarding.md`

## 验证

- [ ] 用 procurement 域纸面演练一遍，确认每步可落地

## 完成标准

- [ ] 文档成文且经一次纸面演练验证可用
