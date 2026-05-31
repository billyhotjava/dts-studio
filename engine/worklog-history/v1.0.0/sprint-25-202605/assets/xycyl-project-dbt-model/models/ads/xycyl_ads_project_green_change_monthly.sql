{{ config(tags=['project-green', 'monthly']) }}

SELECT
  project_id,
  project_code,
  project_name,
  customer_id,
  customer_name,
  pose_month AS stat_month,
  status_code,
  status_label,
  COUNT(*) AS green_row_count,
  SUM(effective_good_number)::numeric(18,2) AS effective_good_number_sum,
  SUM(COALESCE(rent_amount_raw, 0))::numeric(18,2) AS rent_amount_raw_sum,
  SUM(COALESCE(cost_amount_raw, 0))::numeric(18,2) AS cost_amount_raw_sum,
  SUM(COALESCE(rent_amount_adminweb, 0))::numeric(18,2) AS rent_amount_adminweb_sum,
  SUM(COALESCE(cost_amount_adminweb, 0))::numeric(18,2) AS cost_amount_adminweb_sum,
  SUM(COALESCE(real_good_number_adminweb, 0))::numeric(18,2) AS real_good_number_adminweb_sum
FROM {{ ref('xycyl_dwd_project_green_snapshot') }}
GROUP BY
  project_id,
  project_code,
  project_name,
  customer_id,
  customer_name,
  pose_month,
  status_code,
  status_label
