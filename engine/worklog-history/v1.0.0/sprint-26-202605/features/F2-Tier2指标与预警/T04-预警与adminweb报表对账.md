# T04: 预警与 adminweb 固定报表对账

**优先级**: P1
**状态**: DONE
**依赖**: T03

## 目标

把 signals 命中结果与 adminweb 对应固定报表对账，误差 <0.5%，证明预警口径可信。

## 技术设计

- 选取与坏账相关的 adminweb 固定报表 `PRS-FLOWERBIZ-FINANCE-COST` 作为对账面。
- 对同一时间窗，比对本体预警命中集合（命中对象 + 关键指标值）与 adminweb 报表数字。
- 差异分项归因（口径差/孤儿差/时间差），沿用报花域既有对账方法。
- `欠费预警` 当前缺少同源 adminweb 应收/回款固定报表，本任务记录为后续财务/结算口径接入缺口。

## 影响范围

- `it/sql/signal_reconcile_prs_finance_cost.sql` 新增对账 SQL。
- `it/test_signal_reconcile.sh` 新增可重跑脚本。
- `it/evidence/20260530-local/signal-reconcile.md` 新增对账结果。

## 验证

- [x] 关键坏账指标（坏账成本、坏账租金损失、命中项目客户数）对账误差 <0.5%。
- [x] 差异有归因说明。

## 完成标准

- [x] 预警对账证据入 IT，误差达标。
