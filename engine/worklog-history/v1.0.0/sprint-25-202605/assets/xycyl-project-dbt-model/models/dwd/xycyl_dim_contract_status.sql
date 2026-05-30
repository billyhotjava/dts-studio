{{ config(tags=['status-dim']) }}

SELECT *
FROM (VALUES
  ('1', 'CON-DRAFT', '草稿', 1, false),
  ('2', 'CON-ACTIVE', '履行中', 2, true),
  ('3', 'CON-FINISHED', '已结束', 3, false),
  ('UNKNOWN', 'CON-UNKNOWN', '未知', 99, false)
) AS t(code, standard_code, label, sort_order, is_active)
