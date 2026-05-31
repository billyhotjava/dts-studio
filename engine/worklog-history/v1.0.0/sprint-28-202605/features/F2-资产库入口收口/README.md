# F2: 资产库入口收口

**优先级**: P0
**状态**: DONE

## 目标

消除 `/assets` 与旧资产列表路由的双入口、双标题和布局错乱,保留详情/编辑深链。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 抽出资产列表纯内容组件 | P0 | DONE | F1 |
| T02 | 旧列表路由收敛到 `/assets?tab=` | P0 | DONE | T01 |
| T03 | 资产库浏览器回归验证 | P0 | DONE | T01,T02 |

## 完成标准

- [x] `/assets?tab=dashboards/cards/collections` 只有一层资产库标题。
- [x] `/dashboards`、`/questions`、`/collections` 的列表入口行为被明确收敛。
- [x] `/dashboards/:id`、`/questions/:id`、`/collections/:id` 等详情/编辑深链不被破坏。
