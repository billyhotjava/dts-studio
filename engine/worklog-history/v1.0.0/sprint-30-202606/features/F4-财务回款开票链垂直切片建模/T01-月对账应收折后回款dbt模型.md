# T01: 月对账应收/折后/回款 dbt 模型

**优先级**: P1
**状态**: BLOCKED
**依赖**: F2/F3

## 目标

按 ods→stg→dwd→dws→ads 建月对账核心模型,把 `a_month_accounting` + `a_green_accounting` 的三级金额与回款进度规范成可查口径。

## 技术设计

- stg:清洗 `ods_ptr_mysql_a_month_accounting` / `a_green_accounting`(标准化金额列、月份、project_id)。
- dwd:维度对齐(项目/客户/月份 dim),展开 `source_type`(2加/3减/4调减/5调加/6摆位调减/7摆位调加/8销售摊入)。
- dws/ads:`xycyl_ads_finance_month_settlement`——按项目/客户/月输出:名义租金、应收(折前)、折后实收、已回款、回款进度=已回款/折后实收。
- 严格落实 §3.3 三级金额选列、§3.4 销售摊入(source_type=8)防双重计数(标注或剔除)。
- 4 列金额标准对齐 sprint-22 范式(rent/cost/...)。

## 影响范围

- 新建 dbt 模型(finance 命名空间);ads 视图
- dbt schema test(金额非负、回款≤折后实收等)

## 验证

- [ ] ads 产数,关键聚合与业务库直查抽样一致
- [ ] dbt test 绿;source_type=8 不被重复计入

## 完成标准

- [ ] 月对账应收/折后/回款 ads 落地,三级金额口径正确
