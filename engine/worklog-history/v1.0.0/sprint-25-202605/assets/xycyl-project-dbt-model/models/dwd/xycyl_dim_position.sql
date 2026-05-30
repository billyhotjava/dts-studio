{{ config(tags=['position']) }}

SELECT
  p.position_id,
  p.project_id,
  dp.project_code,
  dp.project_name,
  p.position_type_raw,
  p.floor_number_id,
  COALESCE(fn.building_name, p.floor_number_name) AS floor_number_name,
  fn.building_name_full,
  p.floor_layer_id,
  COALESCE(fl.floor_name, p.floor_layer_name) AS floor_layer_name,
  fl.floor_name_full,
  p.position_region,
  p.position_full_name,
  p.curing_period_raw,
  p.follow_type_raw,
  p.contract_dept_id,
  p.contract_dept_name,
  p.curing_user_id,
  p.curing_user_name,
  p.alias_name,
  p.status_raw,
  CASE
    WHEN p.status_raw = 0 THEN 'POS-ACTIVE'
    WHEN p.status_raw = 1 THEN 'POS-INACTIVE'
    ELSE 'POS-UNKNOWN'
  END AS status_code,
  CASE
    WHEN p.status_raw = 0 THEN '正常'
    WHEN p.status_raw = 1 THEN '停用'
    ELSE '未知'
  END AS status_label,
  p.created_at,
  p.updated_at,
  p.source_system,
  p.imported_at
FROM {{ ref('xycyl_stg_project_position') }} p
LEFT JOIN {{ ref('xycyl_dim_project') }} dp ON dp.project_id = p.project_id
LEFT JOIN {{ ref('xycyl_stg_project_floor_number') }} fn ON fn.floor_number_id = p.floor_number_id
LEFT JOIN {{ ref('xycyl_stg_project_floor_layer') }} fl ON fl.floor_layer_id = p.floor_layer_id
