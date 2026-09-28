{{ config(tags=['status-dim']) }}

SELECT *
FROM (VALUES
  ('1', 'PRJ-ACTIVE', '正常', 1, true),
  ('2', 'PRJ-INACTIVE', '停用', 2, false),
  ('UNKNOWN', 'PRJ-UNKNOWN', '未知', 99, false)
) AS t(code, standard_code, label, sort_order, is_active)
