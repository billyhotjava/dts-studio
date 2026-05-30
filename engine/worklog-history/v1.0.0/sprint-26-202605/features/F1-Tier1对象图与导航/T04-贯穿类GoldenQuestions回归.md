# T04: 贯穿类 Golden Questions 回归

**优先级**: P1
**状态**: DONE
**依赖**: T03

## 目标

构建并跑通报花域贯穿类 Golden Questions 集，验证对象图导航命中率 ≥90%。

## 技术设计

示例问句（覆盖 4 条 link）：
- "这个客户从项目到报花再到结算的全链路明细"
- "某项目下所有报花对应的采购单状态"
- "本月某客户的报花有哪些还没结算"
- "某报花单从下单到结算的追溯链"

每条记录：期望路径、期望视图集、期望是否带孤儿提示。

## 影响范围

- `it/sql/`、`it/` 下新增贯穿类问句集与回归脚本。

## 验证

- [x] 命中率 ≥90%（命中=路由到对象图导航且 JOIN 路径正确）。
- [x] 结果保留 sourceRefs / dataSurface / 孤儿提示。
- [x] 证据入 `it/evidence/`。

## 完成标准

- [x] 贯穿类 Golden Questions 回归通过并留证。

## 证据

- `it/sql/flowerbiz_traversal_golden_questions.tsv`
- `it/test_traversal_golden_questions.sh`
- `it/evidence/20260530-local/traversal-golden.md`
