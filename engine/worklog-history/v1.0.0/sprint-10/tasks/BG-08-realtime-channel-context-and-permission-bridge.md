# BG-08: 结算域指标直查对齐

**优先级**: P2
**状态**: READY
**依赖**: BG-02

## 目标

确保所有结算/财务类问题直接查询已计算的结算结果（`a_month_accounting` 等），不让模型尝试从原始绿植数据重算租金，避免口径不一致。

## 问题背景

从 `adminapi` 源码看，月度结算逻辑涉及：

1. **两种计费模式**：
   - `settlement_type=1`：按实摆绿植数 × 单价（`p_project_green.rent`）
   - `settlement_type=2`：固定月租金（`p_contract.month_settlement_money`）

2. **折扣率**：`p_contract.discount_ratio`，默认 1.0

3. **结算周期**：
   - `verify_type=1`：自然月结算
   - `verify_type=2`：固定结算日（如每月 15 日）

4. **跨月分摊**：绿植中途加入/退出需按天分摊当月租金

5. **额外费用**：`t_flower_extra_cost` 中的运费、人工费、清洁费等

这些逻辑写在 Java 的 `AccountingService` 中，**NL2SQL 生成的 SQL 不可能与之保持一致**。

## 技术设计

### 原则

- 凡涉及"租金/应收/已收/欠款/结算"的问题，一律从 `v_monthly_settlement` 查询
- `v_monthly_settlement` 的数据来源是 `a_month_accounting`（已由 Java 计算完成的结果）
- 在意图路由（BG-07）中强制：结算域关键词 → 只注入 `v_monthly_settlement`
- 在 NL2SQL prompt 中显式声明："租金和结算金额请直接从 v_monthly_settlement 获取，不要尝试从绿植数据计算"

### 核心指标对齐

| 指标 | 数据源 | 说明 |
|------|--------|------|
| 月度应收租金 | `v_monthly_settlement.total_rent` | 来自 `a_month_accounting`，已含计费模式和折扣计算 |
| 已收金额 | `v_monthly_settlement.received_amount` | 来自 `a_collection_record` 汇总 |
| 未收金额 | `v_monthly_settlement.outstanding_amount` | = 应收 - 已收 |
| 结算状态 | `v_monthly_settlement.settlement_status_name` | 待结算 / 已结算 |
| 绿植数量 | `v_project_overview.green_count` | 来自 `p_project_green` 实时统计 |
| 绿植月租 | `v_project_overview.total_rent` | 当前在摆绿植的月租金合计（参考值，非结算值） |

### 显式禁止规则

在 NL2SQL 上下文中注入以下约束：

```
【重要约束】
1. 租金、应收、已收、欠款、结算相关问题，必须从 v_monthly_settlement 获取数据
2. 不要使用 v_project_green_current 的 rent 字段来计算月租金
3. v_project_overview.total_rent 是绿植月租参考值，不是实际结算金额
4. 结算金额已包含折扣率、计费模式和跨月分摊的计算
```

### 补充指标：开票相关

如 `a_invoice_info` 在视图层需要暴露，补充：

```sql
-- v_monthly_settlement 可扩展字段
invoice_status_name     -- 开票状态
invoice_amount          -- 已开票金额
```

## 完成标准

- [ ] `v_monthly_settlement` 视图数据来源正确（从 `a_month_accounting`）
- [ ] 意图路由中结算域关键词强制路由到 `v_monthly_settlement`
- [ ] NL2SQL prompt 显式禁止从绿植数据计算租金
- [ ] 结算类预制模板（TPL-09~12）结果与 Java 结算逻辑一致
- [ ] 5+ 条结算类 eval case 通过
