\set ON_ERROR_STOP on
\pset pager off

\echo '== Sprint-25 expected ODS source status =='
WITH expected AS (
  SELECT 'p_customer' AS source_table, 'ods_ptr_mysql_p_customer' AS ods_table, to_regclass('public.ods_ptr_mysql_p_customer') AS relation, true AS required
  UNION ALL SELECT 'p_project', 'ods_ptr_mysql_p_project', to_regclass('public.ods_ptr_mysql_p_project'), true
  UNION ALL SELECT 'p_contract', 'ods_ptr_mysql_p_contract', to_regclass('public.ods_ptr_mysql_p_contract'), true
  UNION ALL SELECT 'p_position', 'ods_ptr_mysql_p_position', to_regclass('public.ods_ptr_mysql_p_position'), true
  UNION ALL SELECT 'p_floor_layer', 'ods_ptr_mysql_p_floor_layer', to_regclass('public.ods_ptr_mysql_p_floor_layer'), true
  UNION ALL SELECT 'p_floor_number', 'ods_ptr_mysql_p_floor_number', to_regclass('public.ods_ptr_mysql_p_floor_number'), true
  UNION ALL SELECT 'b_goods', 'ods_ptr_mysql_b_goods', to_regclass('public.ods_ptr_mysql_b_goods'), true
  UNION ALL SELECT 'b_goods_price', 'ods_ptr_mysql_b_goods_price', to_regclass('public.ods_ptr_mysql_b_goods_price'), true
  UNION ALL SELECT 'p_project_green', 'ods_ptr_mysql_p_project_green', to_regclass('public.ods_ptr_mysql_p_project_green'), true
  UNION ALL SELECT 'p_position_adjustment', 'ods_ptr_mysql_p_position_adjustment', to_regclass('public.ods_ptr_mysql_p_position_adjustment'), true
  UNION ALL SELECT 'p_position_adjustment_item', 'ods_ptr_mysql_p_position_adjustment_item', to_regclass('public.ods_ptr_mysql_p_position_adjustment_item'), true
)
SELECT
  source_table,
  ods_table,
  CASE WHEN relation IS NULL THEN 'MISSING' ELSE 'FOUND' END AS status,
  required
FROM expected
ORDER BY source_table;

\echo '== Metadata columns for found Sprint-25 sources =='
WITH expected(ods_table) AS (
  VALUES
    ('ods_ptr_mysql_p_customer'),
    ('ods_ptr_mysql_p_project'),
    ('ods_ptr_mysql_p_contract'),
    ('ods_ptr_mysql_p_position'),
    ('ods_ptr_mysql_p_floor_layer'),
    ('ods_ptr_mysql_p_floor_number'),
    ('ods_ptr_mysql_b_goods'),
    ('ods_ptr_mysql_b_goods_price'),
    ('ods_ptr_mysql_p_project_green'),
    ('ods_ptr_mysql_p_position_adjustment'),
    ('ods_ptr_mysql_p_position_adjustment_item')
)
SELECT
  e.ods_table,
  COUNT(c.column_name) AS column_count,
  COUNT(*) FILTER (WHERE c.column_name = '_dts_source_system') AS has_dts_source_system,
  COUNT(*) FILTER (WHERE c.column_name = '_dts_import_time') AS has_dts_import_time
FROM expected e
LEFT JOIN information_schema.columns c
  ON c.table_schema = 'public'
 AND c.table_name = e.ods_table
GROUP BY e.ods_table
ORDER BY e.ods_table;

\echo '== Row counts for currently found Sprint-25 sources =='
CREATE TEMP TABLE sprint25_source_row_counts (
  ods_table text,
  row_count bigint
);

WITH expected AS (
  SELECT 'ods_ptr_mysql_p_customer' AS ods_table, to_regclass('public.ods_ptr_mysql_p_customer') AS relation
  UNION ALL SELECT 'ods_ptr_mysql_p_project', to_regclass('public.ods_ptr_mysql_p_project')
  UNION ALL SELECT 'ods_ptr_mysql_p_contract', to_regclass('public.ods_ptr_mysql_p_contract')
  UNION ALL SELECT 'ods_ptr_mysql_p_position', to_regclass('public.ods_ptr_mysql_p_position')
  UNION ALL SELECT 'ods_ptr_mysql_p_floor_layer', to_regclass('public.ods_ptr_mysql_p_floor_layer')
  UNION ALL SELECT 'ods_ptr_mysql_p_floor_number', to_regclass('public.ods_ptr_mysql_p_floor_number')
  UNION ALL SELECT 'ods_ptr_mysql_b_goods', to_regclass('public.ods_ptr_mysql_b_goods')
  UNION ALL SELECT 'ods_ptr_mysql_b_goods_price', to_regclass('public.ods_ptr_mysql_b_goods_price')
  UNION ALL SELECT 'ods_ptr_mysql_p_project_green', to_regclass('public.ods_ptr_mysql_p_project_green')
  UNION ALL SELECT 'ods_ptr_mysql_p_position_adjustment', to_regclass('public.ods_ptr_mysql_p_position_adjustment')
  UNION ALL SELECT 'ods_ptr_mysql_p_position_adjustment_item', to_regclass('public.ods_ptr_mysql_p_position_adjustment_item')
)
SELECT format(
  'INSERT INTO sprint25_source_row_counts SELECT %L AS ods_table, COUNT(*) AS row_count FROM public.%I',
  ods_table,
  ods_table
)
FROM expected
WHERE relation IS NOT NULL
ORDER BY ods_table
\gexec

SELECT ods_table, row_count
FROM sprint25_source_row_counts
ORDER BY ods_table;

\echo '== p_project status distribution =='
SELECT
  COALESCE(status::text, '<NULL>') AS status_raw,
  COUNT(*) AS row_count
FROM public.ods_ptr_mysql_p_project
GROUP BY status_raw
ORDER BY row_count DESC, status_raw;

\echo '== p_customer status distribution =='
SELECT
  COALESCE(status::text, '<NULL>') AS status_raw,
  COUNT(*) AS row_count
FROM public.ods_ptr_mysql_p_customer
GROUP BY status_raw
ORDER BY row_count DESC, status_raw;
