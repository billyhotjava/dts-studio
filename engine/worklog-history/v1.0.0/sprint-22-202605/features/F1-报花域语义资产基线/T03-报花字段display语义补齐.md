# T03: 报花字段 display / semantic 语义补齐（短期方案）

**优先级**: P0
**状态**: READY
**依赖**: T02

## 双轨说明

本 task 是**短期方案**：直接 UPDATE `analytics_table` / `analytics_field`，让报花字段 sprint-22 上线时立即可用。

**长期由 F4-T04 替代**：F4 完成后从 OpenMetadata Catalog API 派生（dbt manifest → OpenMetadata → dts-copilot 启动时拉取）。本 task 写入数据成为兜底。

## 关键 UPDATE

新建 changelog `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_field_semantic.xml`：

### analytics_table

| target_object | display_name | business_description |
|---|---|---|
| `xycyl_ads.xycyl_ads_flowerbiz_lease_summary` | 报花租赁汇总 | 项目 × 月的加摆/撤摆/换花/调花汇总，仅 bizType=1/2/3/4 已结束 |
| `xycyl_ads.xycyl_ads_flowerbiz_lease_detail` | 报花租赁明细 | 单据级明细 |
| `xycyl_ads.xycyl_ads_flowerbiz_pending` | 待处理报花单 | 未结束的报花单含已停留天数 |
| `xycyl_ads.xycyl_ads_flowerbiz_sale_summary` | 报花销售汇总 | bizType=7/8，与租赁分离 |
| `xycyl_ads.xycyl_ads_flowerbiz_baddebt_summary` | 报花坏账汇总 | bizType=6 |
| `xycyl_ads.xycyl_ads_flowerbiz_change_log` | 报花变更日志 | 金额/起租期/规格变更 |
| `xycyl_ads.xycyl_ads_flowerbiz_recovery_detail` | 报花回收明细 | 撤摆/调拨触发 |
| `xycyl_ads.xycyl_ads_flowerbiz_curing_workload` | 养护人报花工作量 | 养护人 × 月 × 业务类别 |

### analytics_field（关键字段）

| field | display_label | synonyms | semantic_type |
|---|---|---|---|
| `项目` / `project_name` | 项目点 | `项目, 项目名, 摆点` | entity_name |
| `客户` / `customer_name` | 客户 | `客户名, 甲方` | entity_name |
| `养护人` / `curing_user` | 养护人 | `养护, 师傅, 养护工` | person_name |
| `业务月份` / `biz_month` | 业务月份 | `月份, 月度` | month_string |
| `加摆金额` / `lease_in_amount` | 加摆金额 | `加摆, 加花金额, 增加金额` | currency |
| `撤摆金额` / `lease_out_amount` | 撤摆金额 | `撤摆, 撤花金额, 减少金额` | currency |
| `净增金额` / `net_amount` | 净增金额 | `净额, 净增减` | currency |
| `销售金额` / `sale_amount` | 销售金额 | `销售` | currency |
| `坏账金额` / `baddebt_amount` | 坏账金额 | `坏账` | currency |
| `回收数量` / `recovery_number` | 回收数量 | `回收` | quantity |
| `实际回收数量` / `real_recovery_number` | 实际回收数量 | `实回, 真实回收` | quantity |
| `已停留天数` / `days_in_status` | 已停留天数 | `停留, 滞留, 挂起` | duration_days |
| `状态` / `status_name` | 状态 | `审核状态, 报花状态` | categorical |
| `业务类型` / `biz_type_name` | 业务类型 | `bizType, 报花类型, 单据类型` | categorical |
| `业务类别` / `biz_category` | 业务类别 | `分类, 类别` | categorical |
| `回收去处` / `recovery_type_name` | 回收去处 | `回收类型, 去处, 处置` | categorical |

## 影响范围

- `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_field_semantic.xml` —— 新增
- `copilot_analytics.analytics_table` —— UPDATE 8 行
- `copilot_analytics.analytics_field` —— UPDATE 约 20 行

## 验证

- [ ] Liquibase 在 dev 库 `mvn liquibase:update` 通过
- [ ] copilot-webapp 字段选择器显示中文 label
- [ ] rollback 脚本可执行

## 完成标准

- [ ] 8 张 ads 视图元数据完整
- [ ] 20+ 字段有中文 label + synonyms
