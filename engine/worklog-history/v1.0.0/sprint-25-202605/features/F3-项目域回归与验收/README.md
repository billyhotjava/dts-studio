# F3: 项目域回归与验收

**优先级**: P1  
**状态**: BLOCKED

## 目标

建立 Sprint-25 的可复现 IT 验收链，覆盖文档结构、dbt 构建、对账和 NL2SQL Golden Questions。

## 阻塞

依赖 F1/F2 产物。当前只保留 F0 source-profile 验收脚本。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 项目域 Golden Questions | P1 | READY | F2 |
| T02 | dbt 与 adminweb 指标对账脚本 | P1 | READY | F1 |
| T03 | IT 证据归档与验收矩阵 | P1 | READY | T01, T02 |

## 完成标准

- [ ] 项目域 Golden Questions ≥ 15 条，其中 ≥ 8 条命中 mart 快路径。
- [ ] mart 命中准确率 ≥ 90%，自由 NL2SQL 准确率 ≥ 75%。
- [ ] `it/evidence/<date>/` 包含命令、日志和结果摘要。
