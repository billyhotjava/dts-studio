# Archived: Finance 版本 sprint-22（已弃用，作为 sprint-25 输入）

## 为什么归档

最初 sprint-22 设计为"财务结算域语义包收口"（基于 sprint-19 ~ sprint-21 已落地的 8 张财务固定报表）。

2026-05 review 报花（flowerbiz）业务后发现：

- 报花单 `t_flower_biz_info` 是业务事实表的源头
- 财务 settlement / month_accounting 是报花的**衍生聚合**
- 不先建立报花域 mart，财务域 mart 都是"空中楼阁"

正确的 sprint 顺序：

```
sprint-22  报花域 (flowerbiz)         ← 现在做
sprint-23  采购域 (purchase)
sprint-24  摆放域 (project_green)
sprint-25  财务域 (settlement)         ← 本归档作为输入
```

## 本目录是什么

最初 sprint-22 的 finance 域产出物：

- `README-original-finance-version.md` —— 原 sprint-22 主 README（finance 版）
- `features/F1-财务域语义资产基线/` —— 4 Task
- `features/F2-财务NL2SQL快路径与planner直达/` —— 3 Task
- `features/F3-财务域回归与验收/` —— 3 Task
- `assets/finance-authority-catalog.md` —— 财务 authority 视图清单 + dbt 模型映射

## sprint-25 时怎么用

- **直接复用**：双轨架构、F1/F2/F3/F4 的工艺范式（验证过有效）
- **重新校对**：sprint-25 启动时，重新校对生产库实际字段口径（数据可能已变化）
- **更新依赖**：sprint-22 / 23 / 24 完成后，财务 mart 的上游可以从"裸 ODS"改为"已建好的报花/采购/摆放 dws"，dbt 模型简化
- **保留双轨架构**：F4 范式不变，新建 `xycyl_*_finance_*` 模型时复用 `assets/dts-stack-dbt-conventions.md` 规范

## 不要做

- ❌ 在本归档目录改文档（任何修改回到主 sprint 目录）
- ❌ 把本目录的 changelog ID 占用（sprint-25 用新的 ID）
