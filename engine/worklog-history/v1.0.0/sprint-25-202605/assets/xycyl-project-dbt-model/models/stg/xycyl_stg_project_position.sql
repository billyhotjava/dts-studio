{{ config(tags=['position']) }}

SELECT
  o.id::bigint AS position_id,
  o.project_id::bigint AS project_id,
  o.type::integer AS position_type_raw,
  o.floor_number_id::bigint AS floor_number_id,
  {{ nullif_placeholder("o.floor_number_name") }} AS floor_number_name,
  o.floor_layer_id::bigint AS floor_layer_id,
  {{ nullif_placeholder("o.floor_layer_name") }} AS floor_layer_name,
  {{ nullif_placeholder("o.region") }} AS position_region,
  {{ nullif_placeholder("o.region_full") }} AS position_full_name,
  {{ nullif_placeholder("o.curing_period") }} AS curing_period_raw,
  {{ nullif_placeholder("o.follow_type") }} AS follow_type_raw,
  {{ nullif_placeholder("o.matters_needing_attention") }} AS matters_needing_attention,
  o.contract_dept_id::bigint AS contract_dept_id,
  {{ nullif_placeholder("o.contract_dept_name") }} AS contract_dept_name,
  o.curing_user_id::bigint AS curing_user_id,
  {{ nullif_placeholder("o.curing_user_name") }} AS curing_user_name,
  {{ nullif_placeholder("o.alias_name") }} AS alias_name,
  o.status::integer AS status_raw,
  o.create_time::timestamp AS created_at,
  o.update_time::timestamp AS updated_at,
  {{ nullif_placeholder("o.remark") }} AS remark,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at
FROM {{ source('xycyl_project_ods', 'p_position') }} o
WHERE COALESCE({{ nullif_placeholder("o.del_flag") }}, '0') = '0'
