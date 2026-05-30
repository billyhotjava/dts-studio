{{ config(tags=['position']) }}

SELECT
  o.id::bigint AS floor_layer_id,
  o.project_id::bigint AS project_id,
  o.floor_number_id::bigint AS floor_number_id,
  {{ nullif_placeholder("o.floor_code") }} AS floor_code,
  {{ nullif_placeholder("o.floor_name") }} AS floor_name,
  {{ nullif_placeholder("o.floor_name_full") }} AS floor_name_full,
  o.create_time::timestamp AS created_at,
  o.update_time::timestamp AS updated_at,
  {{ nullif_placeholder("o.remark") }} AS remark,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at
FROM {{ source('xycyl_project_ods', 'p_floor_layer') }} o
WHERE COALESCE({{ nullif_placeholder("o.del_flag") }}, '0') = '0'
