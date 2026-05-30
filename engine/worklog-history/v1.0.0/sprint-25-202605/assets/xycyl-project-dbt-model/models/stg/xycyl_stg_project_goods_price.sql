{{ config(tags=['goods']) }}

SELECT
  o.id::bigint AS good_price_id,
  o.goods_id::bigint AS goods_id,
  {{ nullif_placeholder("o.goods_code") }} AS goods_code,
  {{ nullif_placeholder("o.goods_name") }} AS goods_name,
  {{ nullif_placeholder("o.specifications") }} AS specifications,
  {{ nullif_placeholder("o.goods_att") }} AS goods_att,
  o.goods_type::integer AS goods_type_raw,
  o.guidance_price::numeric(18,2) AS guidance_price,
  o.cost_price::numeric(18,2) AS cost_price,
  {{ nullif_placeholder("o.unit") }} AS goods_unit,
  {{ nullif_placeholder("o.good_category") }} AS goods_category,
  o.status::integer AS status_raw,
  o.create_time::timestamp AS created_at,
  {{ nullif_placeholder("o.remark") }} AS remark,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at
FROM {{ source('xycyl_project_ods', 'b_goods_price') }} o
WHERE COALESCE({{ nullif_placeholder("o.del_flag") }}, '0') = '0'
