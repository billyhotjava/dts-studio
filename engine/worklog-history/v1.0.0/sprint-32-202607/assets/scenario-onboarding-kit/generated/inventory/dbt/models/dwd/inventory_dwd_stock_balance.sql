-- 库存 DWD: 按 good_price_id/SKU 归一库存余额与成本口径。

with stock as (
  select * from {{ ref('inventory_stg_stock_info') }}
),
goods_price as (
  select * from {{ ref('inventory_stg_goods_price') }}
)

select
  s.stock_id,
  s.storehouse_id,
  coalesce(s.storehouse_name, '未填库房') as storehouse_name,
  s.good_price_id,
  coalesce(s.good_name, gp.goods_name, '未知物品') as good_name,
  coalesce(s.good_norms, gp.specifications, '') as good_norms,
  coalesce(s.good_specs, gp.goods_attribute, '') as good_specs,
  coalesce(s.good_unit, gp.unit, '') as good_unit,
  s.good_type_code,
  gp.goods_type_code,
  gp.good_category,
  s.stock_status_code,
  s.stock_status,
  s.stock_quantity,
  s.stock_unit_cost,
  gp.cost_price as master_cost_price,
  coalesce(nullif(s.stock_unit_cost, 0), nullif(gp.cost_price, 0), 0) as effective_unit_cost,
  s.stock_quantity * coalesce(nullif(s.stock_unit_cost, 0), nullif(gp.cost_price, 0), 0) as effective_cost_amount,
  s.created_at,
  s.updated_at,
  coalesce(s.updated_at, s.created_at) as business_time,
  s.tenant_id,
  case
    when s.stock_quantity < 0 then '负库存'
    when s.stock_quantity <= 2 then '低库存'
    when s.stock_quantity = 0 then '零库存'
    else '正常'
  end as stock_health_status,
  case when gp.good_price_id is null then true else false end as missing_goods_price
from stock s
left join goods_price gp on gp.good_price_id = s.good_price_id
