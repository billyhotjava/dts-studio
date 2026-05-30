# T01: flowerbiz.json 补 links 定义

**优先级**: P0
**状态**: DONE
**依赖**: F0/T01

## 目标

在 flowerbiz.json 的 `links` 节声明报花域对象间的软外键关系，把 10 个孤立对象织成对象图。

## 技术设计

核心链路（软外键，全部 LEFT JOIN + is_orphan 透传）：

| link | from → to | fromKey → toKey | cardinality | joinHint |
|------|-----------|-----------------|-------------|----------|
| 客户_项目 | 客户 → 项目 | 客户编码 → 客户编码 | 1:N | 可能孤儿 |
| 项目_报花 | 项目 → 报花明细 | 项目编码 → 项目编码 | 1:N | - |
| 报花_采购 | 报花明细 → 采购明细 | 报花单id → flower_item_id | 1:N | 采购 flower_item_id 软外键 |
| 报花_结算 | 报花明细 → 结算单 | 报花单id → biz_ids_json | N:1 | biz_ids_json 多报花 JSON 数组需展开 |

每条 link 带 `note` 说明口径与孤儿风险，沿用报花域既有 `is_orphan` 透传约定。

## 影响范围

- `src/main/resources/semantic-packs/flowerbiz.json` 的 `links` 节。
- 同步 `target/classes` 副本由构建产生，不手改。

## 验证

- [x] links 引用的对象名/字段名均存在于现有 objects/synonyms。
- [x] 跨 Sprint-25 共享维度（客户/项目）的 link 标注依赖说明。
- [x] OntologyService 加载后 linkGraph 能取到全部 4 条邻接。

## 完成标准

- [x] 客户→项目→报花→采购→结算链路在 pack 中可声明、可加载。

## 证据

- `it/test_object_graph_join.sh`
- `it/evidence/20260530-local/object-graph-join.md`
