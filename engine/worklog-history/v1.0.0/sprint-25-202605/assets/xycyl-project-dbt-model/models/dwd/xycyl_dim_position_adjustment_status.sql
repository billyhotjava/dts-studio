{{ config(tags=['status-dim', 'position-adjustment']) }}

SELECT *
FROM (VALUES
  ('-1', 'PAS-CANCELLED', '已作废', 1, false),
  ('0', 'PAS-PENDING', '待确认', 2, true),
  ('1', 'PAS-FINISHED', '已结束', 3, false),
  ('10', 'PAS-DRAFT', '草稿', 4, true),
  ('UNKNOWN', 'PAS-UNKNOWN', '未知', 99, false)
) AS t(code, standard_code, label, sort_order, is_active)
