{{ config(tags=['position-adjustment']) }}

SELECT
  o.id::bigint AS position_adjustment_item_id,
  o.position_adjustment_id::bigint AS position_adjustment_id,
  o.old_position_id::bigint AS old_position_id,
  {{ nullif_placeholder("o.old_position_name") }} AS old_position_name,
  {{ nullif_placeholder("o.old_position_full_name") }} AS old_position_full_name,
  o.new_position_id::bigint AS new_position_id,
  {{ nullif_placeholder("o.new_position_name") }} AS new_position_name,
  {{ nullif_placeholder("o.new_position_full_name") }} AS new_position_full_name,
  o.good_price_id::bigint AS good_price_id,
  {{ nullif_placeholder("o.good_name") }} AS good_name,
  {{ nullif_placeholder("o.good_norms") }} AS good_norms,
  {{ nullif_placeholder("o.good_specs") }} AS good_specs,
  o.good_type::integer AS good_type_raw,
  {{ nullif_placeholder("o.good_unit") }} AS good_unit,
  o.adjustment_number::integer AS adjustment_number,
  o.parent_id::bigint AS parent_item_id,
  o.rent::numeric(18,2) AS rent_amount_raw,
  o.green_type::integer AS green_type_raw,
  o.cost::numeric(18,2) AS cost_amount_raw,
  o.old_green_id::bigint AS old_project_green_id,
  COALESCE({{ nullif_placeholder("o._dts_source_system") }}, 'ptr_mysql') AS source_system,
  o._dts_import_time AS imported_at
FROM {{ source('xycyl_project_ods', 'p_position_adjustment_item') }} o
