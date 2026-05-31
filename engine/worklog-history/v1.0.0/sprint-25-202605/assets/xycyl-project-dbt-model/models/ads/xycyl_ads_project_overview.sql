{{ config(tags=['project', 'overview']) }}

WITH position_counts AS (
  SELECT
    project_id,
    COUNT(*) AS position_count
  FROM {{ ref('xycyl_dim_position') }}
  GROUP BY project_id
),
green_totals AS (
  SELECT
    project_id,
    SUM(green_row_count) AS green_row_count,
    SUM(placed_row_count) AS placed_row_count,
    SUM(confirmed_row_count) AS confirmed_row_count,
    SUM(effective_good_number_sum)::numeric(18,2) AS effective_good_number_sum,
    SUM(rent_amount_raw_sum)::numeric(18,2) AS rent_amount_raw_sum,
    SUM(cost_amount_raw_sum)::numeric(18,2) AS cost_amount_raw_sum,
    SUM(rent_amount_adminweb_sum)::numeric(18,2) AS rent_amount_adminweb_sum,
    SUM(cost_amount_adminweb_sum)::numeric(18,2) AS cost_amount_adminweb_sum,
    SUM(real_good_number_adminweb_sum)::numeric(18,2) AS real_good_number_adminweb_sum,
    SUM(green_number_adminweb_sum)::numeric(18,2) AS green_number_adminweb_sum,
    SUM(flowerpot_number_adminweb_sum)::numeric(18,2) AS flowerpot_number_adminweb_sum,
    SUM(flowerrack_number_adminweb_sum)::numeric(18,2) AS flowerrack_number_adminweb_sum
  FROM {{ ref('xycyl_dws_project_green_monthly') }}
  GROUP BY project_id
)
SELECT
  p.project_id,
  p.project_code,
  p.project_name,
  p.status_code AS project_status_code,
  p.status_label AS project_status_label,
  p.customer_id,
  p.customer_name,
  p.contract_id,
  p.contract_code,
  p.contract_title,
  p.project_address,
  p.manager_id,
  p.curing_director_id,
  p.curing_director_name,
  COALESCE(pc.position_count, 0) AS position_count,
  COALESCE(gt.green_row_count, 0) AS green_row_count,
  COALESCE(gt.placed_row_count, 0) AS placed_row_count,
  COALESCE(gt.confirmed_row_count, 0) AS confirmed_row_count,
  COALESCE(gt.effective_good_number_sum, 0)::numeric(18,2) AS effective_good_number_sum,
  COALESCE(gt.rent_amount_raw_sum, 0)::numeric(18,2) AS rent_amount_raw_sum,
  COALESCE(gt.cost_amount_raw_sum, 0)::numeric(18,2) AS cost_amount_raw_sum,
  COALESCE(gt.rent_amount_adminweb_sum, 0)::numeric(18,2) AS rent_amount_adminweb_sum,
  COALESCE(gt.cost_amount_adminweb_sum, 0)::numeric(18,2) AS cost_amount_adminweb_sum,
  COALESCE(gt.real_good_number_adminweb_sum, 0)::numeric(18,2) AS real_good_number_adminweb_sum,
  COALESCE(gt.green_number_adminweb_sum, 0)::numeric(18,2) AS green_number_adminweb_sum,
  COALESCE(gt.flowerpot_number_adminweb_sum, 0)::numeric(18,2) AS flowerpot_number_adminweb_sum,
  COALESCE(gt.flowerrack_number_adminweb_sum, 0)::numeric(18,2) AS flowerrack_number_adminweb_sum,
  p.project_start_time,
  p.project_end_time,
  p.imported_at
FROM {{ ref('xycyl_dim_project') }} p
LEFT JOIN position_counts pc ON pc.project_id = p.project_id
LEFT JOIN green_totals gt ON gt.project_id = p.project_id
