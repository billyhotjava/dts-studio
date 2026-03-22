# T01: 采购待处理与待入库 authority 数据面对齐

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

补齐 `PROC-PURCHASE-REQUEST-TODO` 与 `PROC-PENDING-INBOUND-LIST` 的 authority SQL / L0 数据面。

## 技术设计

- 对齐旧系统页面与控制器：
  - `采购计划明细-待处理`
  - `入库管理-待入库清单`
- 从 `adminapi/adminweb` 锁定真实页面口径、状态口径、时间口径和责任人口径
- 在 `dts-copilot-analytics` 中实现对应 authority 查询
- 关闭两张模板的 `placeholderReviewRequired`

## 影响范围

- `dts-copilot-analytics` fixed report 执行器
- authority SQL / report plan
- 相关 Liquibase 模板增量
- 远程 MySQL 验收样例

## 验证

- [ ] `PROC-PURCHASE-REQUEST-TODO` 远程执行返回 `200`
- [ ] `PROC-PENDING-INBOUND-LIST` 远程执行返回 `200`
- [ ] 模板状态不再是 `placeholderReviewRequired=true`

## 完成标准

- [ ] 两张采购待办/待入库报表都能返回真实业务数据
- [ ] 页面字段与旧系统主页面口径一致
