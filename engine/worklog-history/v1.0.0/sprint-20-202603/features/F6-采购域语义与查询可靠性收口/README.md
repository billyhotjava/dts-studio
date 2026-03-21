# F6: 采购域语义与查询可靠性收口

**优先级**: P0
**状态**: READY

## 目标

基于真实失败案例，把采购域从“裸 schema 猜表”收口成可验证的语义资产与回归链。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 采购域失败案例复盘与 authority SQL 锁定 | P0 | READY | - |
| T02 | 采购域 semantic pack 与字段语义补齐 | P0 | READY | T01 |
| T03 | 采购类 NL2SQL 模板与 planner 直达规则 | P0 | READY | T01, T02 |
| T04 | 远程数据验收样例与 IT 回归 | P1 | READY | T02, T03 |

## 完成标准

- [ ] 采购域失败案例有明确根因、正确表链和 authority SQL 口径
- [ ] `采购人 / 发起人 / 采购金额 / 采购明细` 等问法能映射到稳定语义
- [ ] 采购域典型问句不再落到 `t_purchase_info.title` 或 `i_pendulum_purchase*` 错链
- [ ] `it/` 中存在基于远程真实数据验证的采购回归样例
