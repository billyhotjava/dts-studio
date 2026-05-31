{{ config(tags=['project-green']) }}

SELECT
  g.project_green_id,
  g.project_id,
  p.project_code,
  COALESCE(p.project_name, g.project_name) AS project_name,
  p.customer_id,
  p.customer_code,
  p.customer_name,
  p.contract_id,
  p.contract_code,
  g.position_id,
  pos.position_full_name,
  pos.floor_number_id,
  pos.floor_number_name,
  pos.floor_layer_id,
  pos.floor_layer_name,
  g.good_price_id,
  goods.goods_id,
  COALESCE(goods.goods_name, g.good_name) AS goods_name,
  COALESCE(goods.specifications, g.good_norms) AS goods_norms,
  COALESCE(goods.goods_att, g.good_specs) AS goods_specs,
  COALESCE(goods.goods_unit, g.good_unit) AS goods_unit,
  g.green_type_raw,
  g.good_type_raw,
  g.status_raw,
  COALESCE(s.standard_code, 'PGS-UNKNOWN') AS status_code,
  COALESCE(s.label, '未知') AS status_label,
  g.import_status_raw,
  CASE
    WHEN g.import_status_raw = 1 THEN 'PGI-PENDING'
    WHEN g.import_status_raw = 2 THEN 'PGI-CONFIRMED'
    ELSE 'PGI-UNKNOWN'
  END AS import_status_code,
  g.rent_mode_raw,
  g.rent_amount_raw,
  g.cost_amount_raw,
  g.pose_time,
  g.pose_time::date AS pose_date,
  date_trunc('month', g.pose_time)::date AS pose_month,
  g.parent_project_green_id,
  g.good_number,
  g.total_number,
  COALESCE(g.total_number, g.good_number, 0) AS effective_good_number,
  (g.status_raw = 1 AND g.import_status_raw = 2) AS is_adminweb_project_summary_row,
  CASE
    WHEN g.status_raw = 1 AND g.import_status_raw = 2
      THEN (COALESCE(g.total_number, 0) * COALESCE(g.rent_amount_raw, 0))::numeric(18,2)
    ELSE 0::numeric(18,2)
  END AS rent_amount_adminweb,
  CASE
    WHEN g.status_raw = 1 AND g.import_status_raw = 2
      THEN (COALESCE(g.total_number, 0) * COALESCE(g.cost_amount_raw, 0))::numeric(18,2)
    ELSE 0::numeric(18,2)
  END AS cost_amount_adminweb,
  CASE
    WHEN g.status_raw = 1 AND g.import_status_raw = 2 AND g.parent_project_green_id = -1
      THEN COALESCE(g.good_number, 0)
    ELSE 0
  END AS real_good_number_adminweb,
  CASE
    WHEN g.status_raw = 1 AND g.import_status_raw = 2 AND g.good_type_raw = 1
      THEN COALESCE(g.total_number, 0)
    ELSE 0
  END AS green_number_adminweb,
  CASE
    WHEN g.status_raw = 1 AND g.import_status_raw = 2 AND g.good_type_raw = 2
      THEN COALESCE(g.total_number, 0)
    ELSE 0
  END AS flowerpot_number_adminweb,
  CASE
    WHEN g.status_raw = 1 AND g.import_status_raw = 2 AND g.good_type_raw = 3
      THEN COALESCE(g.total_number, 0)
    ELSE 0
  END AS flowerrack_number_adminweb,
  g.lock_change_number,
  g.lock_cut_number,
  g.lock_transfer_number,
  g.lock_bad_number,
  g.lock_adjustment_number,
  g.last_cost_amount,
  (p.project_id IS NULL) AS is_orphan_project,
  (pos.position_id IS NULL) AS is_orphan_position,
  (goods.good_price_id IS NULL) AS is_orphan_goods_price,
  g.created_at,
  g.updated_at,
  g.source_system,
  g.imported_at
FROM {{ ref('xycyl_stg_project_green') }} g
LEFT JOIN {{ ref('xycyl_dim_project') }} p ON p.project_id = g.project_id
LEFT JOIN {{ ref('xycyl_dim_position') }} pos ON pos.position_id = g.position_id
LEFT JOIN {{ ref('xycyl_dim_goods') }} goods ON goods.good_price_id = g.good_price_id
LEFT JOIN {{ ref('xycyl_dim_project_green_status') }} s ON s.code = COALESCE(g.status_raw::text, 'UNKNOWN')
