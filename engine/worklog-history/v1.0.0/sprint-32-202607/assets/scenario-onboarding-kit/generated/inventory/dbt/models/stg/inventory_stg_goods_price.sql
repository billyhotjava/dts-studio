-- 库存 STG: 物品 SKU/价格主数据。

select
  cast(id as varchar) as good_price_id,
  cast(goods_id as varchar) as goods_id,
  nullif(goods_code, '') as goods_code,
  nullif(goods_name, '') as goods_name,
  nullif(specifications, '') as specifications,
  nullif(goods_att, '') as goods_attribute,
  cast(goods_type as integer) as goods_type_code,
  cast(coalesce(guidance_price, 0) as numeric(18, 4)) as guidance_price,
  cast(coalesce(cost_price, 0) as numeric(18, 4)) as cost_price,
  nullif(unit, '') as unit,
  nullif(good_category, '') as good_category,
  cast(status as integer) as goods_status_code,
  case cast(status as integer)
    when 1 then '已启用'
    when 2 then '已停用'
    else '未知'
  end as goods_status,
  cast(create_time as timestamp) as created_at,
  case when del_flag is null or del_flag = '0' then false else true end as is_deleted
from {{ var('inventory_goods_price_relation', 'mysql.rs_cloud_flower.b_goods_price') }}
where del_flag is null or del_flag = '0'
