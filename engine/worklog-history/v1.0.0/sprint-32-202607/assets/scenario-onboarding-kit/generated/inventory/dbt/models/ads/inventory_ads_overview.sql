-- 库存 ADS: 库存现量表格资产查询面。

select
  business_month as "业务月份",
  storehouse_id as "库房id",
  storehouse_name as "库房",
  good_price_id as "SKU",
  good_name as "物品名称",
  good_norms as "规格",
  good_specs as "物品属性",
  good_unit as "单位",
  good_category as "物品分类",
  stock_status as "库存状态",
  stock_health_status as "库存健康状态",
  total_stock_quantity as "库存数量",
  total_stock_cost_amount as "库存成本金额",
  source_row_count as "源记录数",
  missing_goods_price_count as "缺价格主数据记录数",
  last_business_time as "最近业务时间"
from {{ ref('inventory_dws_stock_monthly') }}
