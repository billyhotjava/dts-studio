# T05: 客户欠款排行 MART 与远程回归收口

**优先级**: P0
**状态**: READY
**依赖**: T04

## 目标

补齐 `FIN-CUSTOMER-AR-RANK` 的真实 MART / 汇总面，并完成 8 张报表的远程回归收口。

## 技术设计

- 审计 `mart.finance.customer_ar_rank_daily`
- 若不存在真实落表，则补齐：
  - 轻量 ELT
  - 或基于 `a_month_accounting` 的权威汇总面
- 远程环境逐条回归 8 张剩余报表
- 更新固定报表目录与 IT 验收文档

## 影响范围

- `mart.finance.customer_ar_rank_daily`
- `FIN-CUSTOMER-AR-RANK`
- `it/` 回归矩阵与远程验收记录

## 验证

- [ ] `FIN-CUSTOMER-AR-RANK` 远程执行返回 `200`
- [ ] 8 张剩余报表有完整远程回归记录

## 完成标准

- [ ] 客户欠款排行不再是 `placeholderReviewRequired=true`
- [ ] 本 sprint 覆盖的 8 张报表全部完成远程环境验收
