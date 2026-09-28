{{ config(tags=['project', 'project-green', 'status']) }}

SELECT
  'project'::text AS status_subject,
  status_code,
  status_label,
  COUNT(*) AS row_count
FROM {{ ref('xycyl_dim_project') }}
GROUP BY status_code, status_label

UNION ALL

SELECT
  'project_green'::text AS status_subject,
  status_code,
  status_label,
  COUNT(*) AS row_count
FROM {{ ref('xycyl_dwd_project_green_snapshot') }}
GROUP BY status_code, status_label
