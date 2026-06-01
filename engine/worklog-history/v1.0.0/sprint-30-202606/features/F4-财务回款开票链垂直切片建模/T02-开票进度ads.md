# T02: 开票进度 ads

**优先级**: P1
**状态**: BLOCKED
**依赖**: T01

## 目标

建开票进度模型,把 `a_invoice_info` / `a_invoice_item` 规范成"申请→已开→已收款"进度与开票金额口径。

## 技术设计

- dwd:展开 `a_invoice_item.biz_type`(1租摆月对账/2销售账单/3绿化/4其他)关联两链事实。
- ads:`xycyl_ads_finance_invoice_progress`——按项目/月输出:申请开票金额、实际开票金额、已回款、开票率、状态分布(1待审批/2开票中/3已开/4已收款)。
- 落实 §3.2 含税口径:销售链含税,租摆链不含税,开票金额标注税率维度(a_invoice_info.tax_rate)。
- 区分 `bill_type`(账单类型)与 `invoice_type`(发票种类,专/普),避免混淆。

## 影响范围

- dbt 模型(finance);ads 视图;dbt test

## 验证

- [ ] ads 产数,开票金额合计与业务库一致
- [ ] biz_type 关联正确(不串表枚举)

## 完成标准

- [ ] 开票进度 ads 落地,含税/账单类型口径清晰
