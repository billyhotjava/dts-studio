{{ config(tags=['position-adjustment']) }}

SELECT
  o.id::bigint AS position_adjustment_id,
  {{ nullif_placeholder("o.code") }} AS adjustment_code,
  {{ nullif_placeholder("o.title") }} AS adjustment_title,
  o.project_id::bigint AS project_id,
  {{ nullif_placeholder("o.project_name") }} AS project_name,
  o.apply_user_id::bigint AS apply_user_id,
  {{ nullif_placeholder("o.apply_user_name") }} AS apply_user_name,
  o.apply_time::timestamp AS apply_time,
  o.status::integer AS status_raw,
  o.total_adjustment_number::integer AS total_adjustment_number,
  o.print_status::integer AS print_status_raw,
  o.print_time::timestamp AS print_time,
  o.print_user_id::bigint AS print_user_id,
  {{ nullif_placeholder("o.print_user_name") }} AS print_user_name,
  o.curing_user_id::bigint AS curing_user_id,
  {{ nullif_placeholder("o.curing_user_name") }} AS curing_user_name,
  o.adjustment_type::integer AS adjustment_type_raw,
  o.task_item_id::bigint AS task_item_id,
  o.tenant_id::bigint AS tenant_id,
  o.create_time::timestamp AS created_at,
  o.update_time::timestamp AS updated_at,
  {{ nullif_placeholder("o.remark") }} AS remark,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at
FROM {{ source('xycyl_project_ods', 'p_position_adjustment') }} o
