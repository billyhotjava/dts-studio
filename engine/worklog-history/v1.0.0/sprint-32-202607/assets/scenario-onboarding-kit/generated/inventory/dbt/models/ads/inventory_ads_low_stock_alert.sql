-- 库存 ADS: 低库存预警清单。
-- low stock rule: 库存数量 <= 2。

select
  "业务月份",
  "库房id",
  "库房",
  "SKU",
  "物品名称",
  "规格",
  "物品属性",
  "单位",
  "物品分类",
  "库存状态",
  "库存健康状态",
  "库存数量",
  "库存成本金额",
  "缺价格主数据记录数",
  case
    when "库存数量" < 0 then 'P0-负库存'
    when "库存数量" = 0 then 'P1-零库存'
    else 'P2-低库存'
  end as "预警级别",
  "最近业务时间"
from {{ ref('inventory_ads_overview') }}
where "库存数量" <= 2
