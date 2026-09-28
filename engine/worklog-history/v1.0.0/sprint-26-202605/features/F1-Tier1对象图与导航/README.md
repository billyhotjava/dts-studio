# F1: Tier1 对象图与导航

**优先级**: P0
**状态**: DONE

## 目标

给报花域 10 个孤立对象织上 Links（客户→项目→报花→采购→结算），让 dts-copilot 从"查一张视图"升级到"沿对象图贯穿追溯"。这是 Tier1 的全部，也是 Tier2 预警和 Tier3 动作的前提。

## 当前进展

F0 的 `OntologyService` 骨架与 schema 扩展已完成。F1 已补齐 Tier1 links，`OntologyService` 能生成对象图 JOIN plan，`AssetBackedPlannerPolicy` 已能将贯穿/追溯类问句路由到对象图导航；Golden Questions 4/4 命中对象图导航，Tier1 已完成。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | flowerbiz.json 补 links 定义 | P0 | DONE | F0/T01 |
| T02 | OntologyService 对象图解析与多表 JOIN 生成 | P0 | DONE | F0/T02, T01 |
| T03 | AssetBackedPlannerPolicy 对象图导航决策分支 | P0 | DONE | T02 |
| T04 | 贯穿类 Golden Questions 回归 | P1 | DONE | T03 |

## 完成标准

- [x] links 覆盖客户/项目/报花/采购/结算软外键链路，标注 cardinality、joinHint、is_orphan 透传。
- [x] OntologyService 能沿 links 生成正确的多表 JOIN SQL（含结算 `biz_ids_json` 多报花展开）。
- [x] planner 能识别"贯穿/全链路/追溯"类问句并路由到对象图导航，而非单视图 NL2SQL。
- [x] 贯穿类 Golden Questions 命中 ≥90%，结果保留 sourceRefs / dataSurface / 孤儿提示。
