-- 库存 DWS: 月度库房 + SKU 库存汇总。

select
  date_trunc('month', business_time) as business_month,
  storehouse_id,
  storehouse_name,
  good_price_id,
  good_name,
  good_norms,
  good_specs,
  good_unit,
  good_type_code,
  good_category,
  stock_status,
  stock_health_status,
  sum(stock_quantity) as total_stock_quantity,
  sum(effective_cost_amount) as total_stock_cost_amount,
  count(*) as source_row_count,
  sum(case when missing_goods_price then 1 else 0 end) as missing_goods_price_count,
  max(business_time) as last_business_time
from {{ ref('inventory_dwd_stock_balance') }}
where business_time is not null
group by
  date_trunc('month', business_time),
  storehouse_id,
  storehouse_name,
  good_price_id,
  good_name,
  good_norms,
  good_specs,
  good_unit,
  good_type_code,
  good_category,
  stock_status,
  stock_health_status
