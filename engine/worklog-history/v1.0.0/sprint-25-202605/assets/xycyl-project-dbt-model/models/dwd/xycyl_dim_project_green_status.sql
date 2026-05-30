{{ config(tags=['status-dim', 'project-green']) }}

SELECT *
FROM (VALUES
  ('1', 'PGS-PLACED', '摆放中', 1, true),
  ('2', 'PGS-CHANGING', '换花中', 2, true),
  ('3', 'PGS-ADDING', '加花中', 3, true),
  ('4', 'PGS-REDUCING', '减花中', 4, true),
  ('5', 'PGS-ADJUSTING', '调花中', 5, true),
  ('6', 'PGS-BADDEBT', '坏账处理中', 6, true),
  ('7', 'PGS-FINISHED', '已结束', 7, false),
  ('UNKNOWN', 'PGS-UNKNOWN', '未知', 99, false)
) AS t(code, standard_code, label, sort_order, is_active)
