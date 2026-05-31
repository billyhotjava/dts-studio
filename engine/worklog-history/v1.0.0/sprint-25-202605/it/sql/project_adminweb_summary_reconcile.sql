\set ON_ERROR_STOP on
\pset pager off

\echo '== Sprint-25 adminweb ProjectSummary reconciliation =='
\echo 'Source: adminapi/rs-modules/rs-flowers-base/src/main/resources/mapper/statistics/ProjectSummaryMapper.xml'
\echo 'Endpoint: adminweb /rs-flowers-base/statistics/projectSummary/listPage'

WITH adminweb_project_summary AS (
  SELECT
    COUNT(DISTINCT p.id)::numeric(18,2) AS project_count,
    COALESCE(SUM(CASE
      WHEN g.status = 1 AND g.import_status = 2
        THEN COALESCE(g.total_number, 0) * COALESCE(g.rent, 0)
      ELSE 0
    END), 0)::numeric(18,2) AS rent_amount_adminweb_sum,
    COALESCE(SUM(CASE
      WHEN g.status = 1 AND g.import_status = 2
        THEN COALESCE(g.total_number, 0) * COALESCE(g.cost, 0)
      ELSE 0
    END), 0)::numeric(18,2) AS cost_amount_adminweb_sum,
    COALESCE(SUM(CASE
      WHEN g.status = 1 AND g.import_status = 2 AND g.parent_id = -1
        THEN COALESCE(g.good_number, 0)
      ELSE 0
    END), 0)::numeric(18,2) AS real_good_number_adminweb_sum,
    COALESCE(SUM(CASE
      WHEN g.status = 1 AND g.import_status = 2 AND g.good_type = 1
        THEN COALESCE(g.total_number, 0)
      ELSE 0
    END), 0)::numeric(18,2) AS green_number_adminweb_sum,
    COALESCE(SUM(CASE
      WHEN g.status = 1 AND g.import_status = 2 AND g.good_type = 2
        THEN COALESCE(g.total_number, 0)
      ELSE 0
    END), 0)::numeric(18,2) AS flowerpot_number_adminweb_sum,
    COALESCE(SUM(CASE
      WHEN g.status = 1 AND g.import_status = 2 AND g.good_type = 3
        THEN COALESCE(g.total_number, 0)
      ELSE 0
    END), 0)::numeric(18,2) AS flowerrack_number_adminweb_sum
  FROM public.ods_ptr_mysql_p_project p
  LEFT JOIN public.ods_ptr_mysql_p_project_green g ON g.project_id = p.id
  WHERE COALESCE(p.del_flag, '0') = '0'
),
dbt_project_overview AS (
  SELECT
    COUNT(*)::numeric(18,2) AS project_count,
    COALESCE(SUM(rent_amount_adminweb_sum), 0)::numeric(18,2) AS rent_amount_adminweb_sum,
    COALESCE(SUM(cost_amount_adminweb_sum), 0)::numeric(18,2) AS cost_amount_adminweb_sum,
    COALESCE(SUM(real_good_number_adminweb_sum), 0)::numeric(18,2) AS real_good_number_adminweb_sum,
    COALESCE(SUM(green_number_adminweb_sum), 0)::numeric(18,2) AS green_number_adminweb_sum,
    COALESCE(SUM(flowerpot_number_adminweb_sum), 0)::numeric(18,2) AS flowerpot_number_adminweb_sum,
    COALESCE(SUM(flowerrack_number_adminweb_sum), 0)::numeric(18,2) AS flowerrack_number_adminweb_sum
  FROM public.xycyl_ads_project_overview
),
comparisons AS (
  SELECT 'project_count' AS metric, a.project_count AS adminweb_value, d.project_count AS dbt_value FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
  UNION ALL SELECT 'rent_amount_adminweb_sum', a.rent_amount_adminweb_sum, d.rent_amount_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
  UNION ALL SELECT 'cost_amount_adminweb_sum', a.cost_amount_adminweb_sum, d.cost_amount_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
  UNION ALL SELECT 'real_good_number_adminweb_sum', a.real_good_number_adminweb_sum, d.real_good_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
  UNION ALL SELECT 'green_number_adminweb_sum', a.green_number_adminweb_sum, d.green_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
  UNION ALL SELECT 'flowerpot_number_adminweb_sum', a.flowerpot_number_adminweb_sum, d.flowerpot_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
  UNION ALL SELECT 'flowerrack_number_adminweb_sum', a.flowerrack_number_adminweb_sum, d.flowerrack_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
),
scored AS (
  SELECT
    metric,
    adminweb_value,
    dbt_value,
    ABS(adminweb_value - dbt_value)::numeric(18,4) AS diff_abs,
    CASE
      WHEN ABS(adminweb_value) = 0 THEN CASE WHEN ABS(dbt_value) = 0 THEN 0::numeric ELSE 1::numeric END
      ELSE (ABS(adminweb_value - dbt_value) / ABS(adminweb_value))::numeric
    END AS diff_pct
  FROM comparisons
)
SELECT
  metric,
  adminweb_value,
  dbt_value,
  diff_abs,
  ROUND(diff_pct * 100, 4) AS diff_pct,
  CASE WHEN diff_pct <= 0.005 THEN 'PASS' ELSE 'FAIL' END AS status
FROM scored
ORDER BY metric;

DO $$
DECLARE
  failure_count integer;
BEGIN
  WITH adminweb_project_summary AS (
    SELECT
      COUNT(DISTINCT p.id)::numeric(18,2) AS project_count,
      COALESCE(SUM(CASE WHEN g.status = 1 AND g.import_status = 2 THEN COALESCE(g.total_number, 0) * COALESCE(g.rent, 0) ELSE 0 END), 0)::numeric(18,2) AS rent_amount_adminweb_sum,
      COALESCE(SUM(CASE WHEN g.status = 1 AND g.import_status = 2 THEN COALESCE(g.total_number, 0) * COALESCE(g.cost, 0) ELSE 0 END), 0)::numeric(18,2) AS cost_amount_adminweb_sum,
      COALESCE(SUM(CASE WHEN g.status = 1 AND g.import_status = 2 AND g.parent_id = -1 THEN COALESCE(g.good_number, 0) ELSE 0 END), 0)::numeric(18,2) AS real_good_number_adminweb_sum,
      COALESCE(SUM(CASE WHEN g.status = 1 AND g.import_status = 2 AND g.good_type = 1 THEN COALESCE(g.total_number, 0) ELSE 0 END), 0)::numeric(18,2) AS green_number_adminweb_sum,
      COALESCE(SUM(CASE WHEN g.status = 1 AND g.import_status = 2 AND g.good_type = 2 THEN COALESCE(g.total_number, 0) ELSE 0 END), 0)::numeric(18,2) AS flowerpot_number_adminweb_sum,
      COALESCE(SUM(CASE WHEN g.status = 1 AND g.import_status = 2 AND g.good_type = 3 THEN COALESCE(g.total_number, 0) ELSE 0 END), 0)::numeric(18,2) AS flowerrack_number_adminweb_sum
    FROM public.ods_ptr_mysql_p_project p
    LEFT JOIN public.ods_ptr_mysql_p_project_green g ON g.project_id = p.id
    WHERE COALESCE(p.del_flag, '0') = '0'
  ),
  dbt_project_overview AS (
    SELECT
      COUNT(*)::numeric(18,2) AS project_count,
      COALESCE(SUM(rent_amount_adminweb_sum), 0)::numeric(18,2) AS rent_amount_adminweb_sum,
      COALESCE(SUM(cost_amount_adminweb_sum), 0)::numeric(18,2) AS cost_amount_adminweb_sum,
      COALESCE(SUM(real_good_number_adminweb_sum), 0)::numeric(18,2) AS real_good_number_adminweb_sum,
      COALESCE(SUM(green_number_adminweb_sum), 0)::numeric(18,2) AS green_number_adminweb_sum,
      COALESCE(SUM(flowerpot_number_adminweb_sum), 0)::numeric(18,2) AS flowerpot_number_adminweb_sum,
      COALESCE(SUM(flowerrack_number_adminweb_sum), 0)::numeric(18,2) AS flowerrack_number_adminweb_sum
    FROM public.xycyl_ads_project_overview
  ),
  comparisons AS (
    SELECT 'project_count' AS metric, a.project_count AS adminweb_value, d.project_count AS dbt_value FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
    UNION ALL SELECT 'rent_amount_adminweb_sum', a.rent_amount_adminweb_sum, d.rent_amount_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
    UNION ALL SELECT 'cost_amount_adminweb_sum', a.cost_amount_adminweb_sum, d.cost_amount_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
    UNION ALL SELECT 'real_good_number_adminweb_sum', a.real_good_number_adminweb_sum, d.real_good_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
    UNION ALL SELECT 'green_number_adminweb_sum', a.green_number_adminweb_sum, d.green_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
    UNION ALL SELECT 'flowerpot_number_adminweb_sum', a.flowerpot_number_adminweb_sum, d.flowerpot_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
    UNION ALL SELECT 'flowerrack_number_adminweb_sum', a.flowerrack_number_adminweb_sum, d.flowerrack_number_adminweb_sum FROM adminweb_project_summary a CROSS JOIN dbt_project_overview d
  ),
  scored AS (
    SELECT
      metric,
      CASE
        WHEN ABS(adminweb_value) = 0 THEN CASE WHEN ABS(dbt_value) = 0 THEN 0::numeric ELSE 1::numeric END
        ELSE (ABS(adminweb_value - dbt_value) / ABS(adminweb_value))::numeric
      END AS diff_pct
    FROM comparisons
  )
  SELECT COUNT(*) INTO failure_count
  FROM scored
  WHERE diff_pct > 0.005;

  IF failure_count > 0 THEN
    RAISE EXCEPTION 'project adminweb reconciliation failed: % metric(s) over 0.5%%', failure_count;
  END IF;
END $$;
