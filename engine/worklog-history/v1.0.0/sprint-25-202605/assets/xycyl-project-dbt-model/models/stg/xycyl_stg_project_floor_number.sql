{{ config(tags=['position']) }}

SELECT
  o.id::bigint AS floor_number_id,
  o.project_id::bigint AS project_id,
  {{ nullif_placeholder("o.building_code") }} AS building_code,
  {{ nullif_placeholder("o.building_name") }} AS building_name,
  {{ nullif_placeholder("o.building_name_full") }} AS building_name_full,
  o.create_time::timestamp AS created_at,
  o.update_time::timestamp AS updated_at,
  {{ nullif_placeholder("o.remark") }} AS remark,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at
FROM {{ source('xycyl_project_ods', 'p_floor_number') }} o
WHERE COALESCE({{ nullif_placeholder("o.del_flag") }}, '0') = '0'
