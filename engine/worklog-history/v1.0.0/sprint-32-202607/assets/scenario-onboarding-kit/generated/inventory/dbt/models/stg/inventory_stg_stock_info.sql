-- 库存 STG: 库存余额主表字段类型化。
-- 默认 relation 为 Trino 三段式；如导入 ODS 后在 Postgres dbt 构建，可通过 var 覆盖为 ODS 表。

select
  cast(id as varchar) as stock_id,
  cast(storehouse_info_id as varchar) as storehouse_id,
  nullif(storehouse_name, '') as storehouse_name,
  cast(good_price_id as varchar) as good_price_id,
  nullif(good_name, '') as good_name,
  cast(good_type as integer) as good_type_code,
  nullif(good_norms, '') as good_norms,
  nullif(good_specs, '') as good_specs,
  nullif(good_unit, '') as good_unit,
  cast(status as integer) as stock_status_code,
  case cast(status as integer)
    when 1 then '已启用'
    when 2 then '已停用'
    else '未知'
  end as stock_status,
  cast(coalesce(good_number, 0) as numeric(18, 4)) as stock_quantity,
  cast(coalesce(out_cost, 0) as numeric(18, 4)) as stock_unit_cost,
  cast(coalesce(good_number, 0) as numeric(18, 4))
    * cast(coalesce(out_cost, 0) as numeric(18, 4)) as stock_cost_amount,
  cast(create_time as timestamp) as created_at,
  cast(update_time as timestamp) as updated_at,
  coalesce(cast(update_time as timestamp), cast(create_time as timestamp)) as business_time,
  cast(tenant_id as varchar) as tenant_id,
  case when del_flag is null or del_flag = '0' then false else true end as is_deleted
from {{ var('inventory_stock_info_relation', 'mysql.rs_cloud_flower.s_stock_info') }}
where del_flag is null or del_flag = '0'
