# T01: 采购域失败案例复盘与 authority SQL 锁定

**优先级**: P0
**状态**: DONE
**依赖**: 无

## 目标

把演示失败案例收敛成一条可复用的 authority SQL 口径，而不是继续靠现场猜表。

## 技术设计

- 复盘真实失败问句：`查询2025年2月，绿萝这个产品的采购详细情况，按采购人、采购金额统计`
- 对照 `adminweb / adminapi / rs_cloud_flower`，锁定旧系统真实采购明细页与 Mapper SQL
- 固化正确表链：
  - `t_purchase_price_item`
  - `t_purchase_info`
  - `t_plan_purchase_item`
  - `t_flower_biz_item`
  - `t_flower_biz_info`
- 明确禁止继续使用的错链：
  - `t_purchase_info.title like '%绿萝%'`
  - `i_pendulum_purchase*`
- 产出采购域 authority SQL 说明，作为 semantic pack、template 和 IT 的共同基准

## 影响范围

- `adminapi/rs-modules/rs-flowers-base/src/main/resources/mapper/purchase/PurchasePriceItemMapper.xml`
- `adminweb/src/views/flower/purchase/purchaseitem/list-purchase-good.vue`
- `worklog/v1.0.0/sprint-20-202603/features/F6-采购域语义与查询可靠性收口/`
- `worklog/v1.0.0/sprint-20-202603/it/procurement-query-regression.md`

## 验证

- [x] authority SQL 与旧系统采购明细页字段口径一致
- [x] 能解释为什么 `t_purchase_info.title` 与 `i_pendulum_purchase*` 会查不到 `2025-02 绿萝`

## 完成标准

- [x] 采购域 authority SQL 和正确表链被明确写入 sprint 文档
- [x] 失败案例根因不再停留在“模型没答对”，而是落到可实现的取数设计
