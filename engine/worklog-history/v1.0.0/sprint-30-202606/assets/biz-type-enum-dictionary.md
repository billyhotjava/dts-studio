# Sprint-30 biz_type / status 枚举字典

本字典用于 F3 `CAL-BIZTYPE-SCOPE` 护栏。结论先行：`biz_type` 不是全局枚举，同名字段必须按表名解释；跨表 JOIN、过滤、汇总前必须显式写出 `table.field` 与业务链路。

## 报花主单与明细

| 字段 | 代码 | 含义 | 来源 |
|------|------|------|------|
| `t_flower_biz_info.biz_type` | 1 | 换花 | `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_008__biz_enum_dictionary.xml:130-143`; `adminapi/docker/mysql/db/rs_cloud_flower.sql:2971-2981` |
| `t_flower_biz_info.biz_type` | 2 | 加花 | 同上 |
| `t_flower_biz_info.biz_type` | 3 | 减花 | 同上 |
| `t_flower_biz_info.biz_type` | 4 | 调花 | 同上 |
| `t_flower_biz_info.biz_type` | 5 | 售花 | 同上；编号生成函数也使用 5=售 |
| `t_flower_biz_info.biz_type` | 6 | 坏账/赠送歧义 | 字典 seed 为坏账；DDL 注释/编号函数中 6=赠，见 `adminapi/docker/mysql/db/rs_cloud_flower.sql:2977` 与 `FlowerBizInfoServiceImpl.getNewFlowerBizCode` |
| `t_flower_biz_info.biz_type` | 7 | 销售/坏账歧义 | 字典 seed 为销售；编号函数中 7=坏，见 `FlowerBizInfoServiceImpl.getNewFlowerBizCode` |
| `t_flower_biz_info.biz_type` | 8 | 内购 | `v1_0_0_008__biz_enum_dictionary.xml:141`; 运营地图 §3.4 将 `source_type=8` 标为售花摊入租摆 |
| `t_flower_biz_info.biz_type` | 11/12 | 加盆架/减盆架 | `v1_0_0_008__biz_enum_dictionary.xml:142-143` |
| `t_flower_biz_item.biz_type` | 1 | 调花 | `adminapi/docker/mysql/db/rs_cloud_flower.sql:3064-3072` |
| `t_flower_biz_item.biz_type` | 2 | 加花 | 同上 |
| `t_flower_biz_item.biz_type` | 3 | 减花 | 同上 |
| `t_flower_biz_item.biz_type` | 4 | 调花减花 | 同上 |
| `t_flower_biz_item.biz_type` | 5 | 调花加花 | 同上 |
| `FlowerBizInfoServiceImpl.getNewFlowerBizCode` | 1/2/3/4/5/6/7 | 换/加/减/调/售/赠/坏 | `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/flowerbiz/service/impl/FlowerBizInfoServiceImpl.java:562-578` |

## 财务开票与收款

| 字段 | 代码 | 含义 | 来源 |
|------|------|------|------|
| `a_invoice_item.biz_type` | 1 | 租摆应收/月对账 | `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/operate/service/impl/InvoiceInfoServiceImpl.java:321-331`; 运营地图 §3.1 |
| `a_invoice_item.biz_type` | 2 | 销售账单 | 同上 |
| `a_invoice_item.biz_type` | 3 | 绿化养护 | 同上 |
| `a_invoice_item.biz_type` | 4 | 其他项目/自定义项 | 同上 |
| `a_collection_item.biz_type` | 业务来源类型 | 独立收款来源枚举，不能套用报花主单枚举 | `adminapi/docker/mysql/db/rs_cloud_flower.sql:217-223`; 运营地图 §3.1 |
| `a_sale_account.status` | 1/2/3 | 待结算/开票中/已回款 | `dts-copilot/docs/business/xycyl-operational-domain-map.md:70` |
| `a_month_accounting.status` | 1/2/3 | 待生成/已生成/已结算 | `adminapi/docker/mysql/db/rs_cloud_flower.sql:3032`; 结算链见运营地图 §3.2 |

## 仓储、出库与月对账来源

| 字段 | 代码 | 含义 | 来源 |
|------|------|------|------|
| `t_warehousing_info.warehousing_type` | 业务自定义 | 减花入库/自主采购入库 | `adminapi/docker/mysql/db/rs_cloud_flower.sql:4125-4135` |
| `t_ex_warehouse_info.out_house_type` | 业务自定义 | 出库类型，独立于报花 `biz_type` | `adminapi/docker/mysql/db/rs_cloud_flower.sql:2901-2928` |
| `a_green_accounting.source_type` | 1/8 | 期初；8 也按期初展示但在运营地图中标识售花摊入租摆风险 | `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/operate/service/impl/AccountingExportServiceImpl.java:545-568` |
| `a_green_accounting.source_type` | 2 | 加花 | 同上 |
| `a_green_accounting.source_type` | 3 | 减花 | 同上 |
| `a_green_accounting.source_type` | 4 | 调减 | 同上 |
| `a_green_accounting.source_type` | 5 | 调加 | 同上 |
| `a_green_accounting.source_type` | 6/7 | 摆位调减/摆位调加 | 源码当前两段条件都写 `6`，应按风险处理并回查源数据；见 `AccountingExportServiceImpl.java:562-568` |

## 机器护栏映射

| guardrail | 字典字段 | 约束 |
|-----------|----------|------|
| `CAL-BIZTYPE-SCOPE` | 全部同名 `biz_type` / `source_type` / 仓储类型 | SQL 生成必须写出 `table.field` 和枚举来源。 |
| `CAL-SETTLEMENT-CHAIN` | `a_invoice_item.biz_type`, `a_month_accounting`, `a_sale_account` | 租摆链、售赠坏链不可直接混合 SUM。 |
| `CAL-SALE-IN-RENT` | `a_green_accounting.source_type=8` | 跨租摆和销售链统计必须剔除或单列，防双重计数。 |
