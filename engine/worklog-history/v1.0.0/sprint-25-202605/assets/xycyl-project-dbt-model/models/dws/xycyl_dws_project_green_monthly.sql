{{ config(tags=['project-green', 'monthly']) }}

SELECT
  g.project_id,
  g.project_code,
  g.project_name,
  g.customer_id,
  g.customer_name,
  g.pose_month AS stat_month,
  COUNT(*) AS green_row_count,
  COUNT(*) FILTER (WHERE g.status_code = 'PGS-PLACED') AS placed_row_count,
  COUNT(*) FILTER (WHERE g.import_status_code = 'PGI-CONFIRMED') AS confirmed_row_count,
  SUM(g.effective_good_number)::numeric(18,2) AS effective_good_number_sum,
  SUM(COALESCE(g.total_number, 0))::numeric(18,2) AS total_number_sum,
  SUM(COALESCE(g.good_number, 0))::numeric(18,2) AS good_number_sum,
  SUM(COALESCE(g.rent_amount_raw, 0))::numeric(18,2) AS rent_amount_raw_sum,
  SUM(COALESCE(g.cost_amount_raw, 0))::numeric(18,2) AS cost_amount_raw_sum,
  SUM(COALESCE(g.lock_change_number, 0))::numeric(18,2) AS lock_change_number_sum,
  SUM(COALESCE(g.lock_cut_number, 0))::numeric(18,2) AS lock_cut_number_sum,
  SUM(COALESCE(g.lock_transfer_number, 0))::numeric(18,2) AS lock_transfer_number_sum,
  SUM(COALESCE(g.lock_bad_number, 0))::numeric(18,2) AS lock_bad_number_sum,
  SUM(COALESCE(g.lock_adjustment_number, 0))::numeric(18,2) AS lock_adjustment_number_sum,
  COUNT(*) FILTER (WHERE g.is_orphan_project) AS orphan_project_count,
  COUNT(*) FILTER (WHERE g.is_orphan_position) AS orphan_position_count,
  COUNT(*) FILTER (WHERE g.is_orphan_goods_price) AS orphan_goods_price_count
FROM {{ ref('xycyl_dwd_project_green_snapshot') }} g
GROUP BY
  g.project_id,
  g.project_code,
  g.project_name,
  g.customer_id,
  g.customer_name,
  g.pose_month
