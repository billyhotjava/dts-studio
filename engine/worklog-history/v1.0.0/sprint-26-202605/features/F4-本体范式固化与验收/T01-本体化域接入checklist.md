# T01: 本体化域接入 checklist

**优先级**: P2
**状态**: DONE
**依赖**: F1, F2, F3

## 目标

把报花域走通的本体三层抽象成可复用 checklist，让项目/采购/财务域能照单接入。

## 技术设计

checklist 至少覆盖：

1. **pack 扩展**：补 links（识别软外键链路 + joinHint）、metrics（口径对齐 dbt 注释）、signals（阈值 + advice + linkedActions）、actions（映射 adminapi 端点）。
2. **Java 扩展点**：OntologyService 通常无需改（数据驱动）；planner 分支若已通用则零改；新域仅在 PACK_FILES / normalizeSemanticDomain / catalog ENTRIES 等已知点追加。
3. **adminapi 端点盘点**：新域要写回前，先确认对应"草稿+正式"双端点是否存在；缺则记为接口需求。
4. **对账面**：列出该域需对账的 adminweb 固定报表。
5. **Golden Questions**：贯穿类 + 预警类 + 动作类各准备问句集。

## 影响范围

- `assets/` 下新增 `ontology-domain-onboarding-checklist.md`。

## 验证

- [x] checklist 用项目域做一次"纸面演练"，确认可操作、无遗漏。

## 完成标准

- [x] checklist 可被后续 sprint 直接引用。

## 产出物

- `assets/ontology-domain-onboarding-checklist.md`
- `it/test_ontology_onboarding_checklist.sh`
- `it/evidence/20260530-local/ontology-onboarding-checklist.md`
