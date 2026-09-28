{{ config(tags=['goods']) }}

SELECT
  o.id::bigint AS goods_id,
  {{ nullif_placeholder("o.name") }} AS goods_name,
  {{ nullif_placeholder("o.code") }} AS goods_code,
  o.type::integer AS goods_type_raw,
  {{ nullif_placeholder("o.unit") }} AS goods_unit,
  {{ nullif_placeholder("o.specifications") }} AS specifications,
  o.status::integer AS status_raw,
  {{ nullif_placeholder("o.category") }} AS goods_category,
  o.goods_classify_id::bigint AS goods_classify_id,
  o.tenant_id::bigint AS tenant_id,
  o.create_time::timestamp AS created_at,
  o.update_time::timestamp AS updated_at,
  {{ nullif_placeholder("o.remark") }} AS remark,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at
FROM {{ source('xycyl_project_ods', 'b_goods') }} o
WHERE COALESCE({{ nullif_placeholder("o.del_flag") }}, '0') = '0'
