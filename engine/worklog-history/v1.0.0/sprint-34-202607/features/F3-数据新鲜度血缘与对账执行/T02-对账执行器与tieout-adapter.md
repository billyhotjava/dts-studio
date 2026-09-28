# T02: 对账执行器与 tie-out adapter

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

对 NL2SQL 结果执行可配置对账，支持 count、sum、主键去重、借贷平衡、权威基准端点对比。

## 技术设计

- `TieoutCheck` 输入：target relation、过滤条件、metric、authority relation/endpoint。
- 输出：`PASS`、`DRIFT`、`FAIL`、`SKIPPED`。
- 财务凭证首个样例：STG 凭证数 = ADS 凭证数，STG 有效分录数 = ADS 分录数。

## 影响范围

- finance voucher、finance yearly、flowerbiz order 黄金样例。
- evidence tieout section。

## 验证

- [x] 2026 凭证 ADS 的 `凭证数` 与 STG 主表数量一致。
- [x] 分录数与 STG 有效分录数量一致。
- [x] 差异非 0 时不能标 HIGH。

## 完成标准

- [x] tie-out adapter 可被黄金集和运行时 evidence 复用。
