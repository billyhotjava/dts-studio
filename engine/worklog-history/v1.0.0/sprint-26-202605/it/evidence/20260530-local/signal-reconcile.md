# Sprint-26 F2/T04 预警与 adminweb 固定报表对账

**时间**: 2026-05-30
**范围**: `坏账风险` signal 命中集合，对账 adminweb 已认证固定报表 `PRS-FLOWERBIZ-FINANCE-COST` / `prs-flowerbiz-finance-cost-v1`。

## 对账口径

- 时间窗：`2025-05-01` 至 `2026-05-30`，按 `业务月份` 对齐。
- signal 来源：`public.xycyl_ads_flowerbiz_baddebt_summary`。
- adminweb 固定报表来源：`public.xycyl_ads_flowerbiz_finance_cost`，对应固定报表资产 `PRS-FLOWERBIZ-FINANCE-COST`，坏账拆分资产 `PRS-FLOWERBIZ-FINANCE-BADDEBT`。
- 阈值：`项目坏账率 > 0.15 AND 坏账租金损失 > 0`。
- 达标线：误差率 `<= 0.5%`。

## RED 证据

命令：

```bash
test -f worklog/v1.0.0/sprint-26-202605/it/sql/signal_reconcile_prs_finance_cost.sql
```

结果：失败，T04 无可重跑 SQL 和脚本证据。

## GREEN 证据

命令：

```bash
RUN_DB=1 worklog/v1.0.0/sprint-26-202605/it/test_signal_reconcile.sh
```

结果：

```text
[static] signal reconciliation SQL is aligned with PRS finance fixed report assets
坏账成本全口径|6610.79|6610.79|0.00|0.0000|PASS
坏账租金损失全口径|2724.92|2724.92|0.00|0.0000|PASS
坏账风险命中项目客户数|27.00|27.00|0.00|0.0000|PASS
[db] signal reconciliation passed within 0.5% threshold
```

## 差异归因

- 坏账 signal 与 adminweb 固定报表均落在 PRS dbt ADS 层，且按同一时间窗和同一阈值切片，因此本次误差为 0。
- `欠费预警` 当前使用 `客户在租金额 + 坏账租金损失` 的组合口径；adminweb 当前 PRS 固定报表未提供同源的应收/回款字段，不能把它解释为真实回款对账。本项记录为后续财务/结算口径接入缺口，不影响坏账风险闭环。
