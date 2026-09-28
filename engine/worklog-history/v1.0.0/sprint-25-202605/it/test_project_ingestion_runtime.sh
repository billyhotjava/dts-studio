#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
SQL_FILE="$ROOT_DIR/worklog/v1.0.0/sprint-25-202605/it/sql/project_ingestion_task_upsert.sql"

require_text() {
  local needle="$1"
  local file="$2"
  if ! grep -Fq "$needle" "$file"; then
    echo "[missing] $needle in $file" >&2
    exit 1
  fi
}

require_text "sprint25_project_datasurface" "$SQL_FILE"
require_text "p_project_green" "$SQL_FILE"
require_text "p_position_adjustment_item" "$SQL_FILE"
require_text "ods_ptr_mysql_p_project_green" "$SQL_FILE"
require_text "ptr_mysql_flow" "$SQL_FILE"

echo "[static] sprint25 project ingestion task upsert SQL is present"

if [[ "${RUN_LIVE:-0}" != "1" ]]; then
  exit 0
fi

: "${DTS_PG_CONTAINER:=v223-dts-pg-1}"
: "${DTS_PLATFORM_USER:=dts_platform}"
: "${DTS_PLATFORM_DB:=dts_platform}"
: "${DTS_BI_USER:=biadmin}"
: "${DTS_BI_DB:=biadmin}"

task_check=$(
  docker exec "$DTS_PG_CONTAINER" psql -U "$DTS_PLATFORM_USER" -d "$DTS_PLATFORM_DB" -Atc "
WITH required(source_table, target_table) AS (
  VALUES
    ('p_project', 'ods_ptr_mysql_p_project'),
    ('p_customer', 'ods_ptr_mysql_p_customer'),
    ('p_contract', 'ods_ptr_mysql_p_contract'),
    ('p_position', 'ods_ptr_mysql_p_position'),
    ('p_floor_layer', 'ods_ptr_mysql_p_floor_layer'),
    ('p_floor_number', 'ods_ptr_mysql_p_floor_number'),
    ('b_goods', 'ods_ptr_mysql_b_goods'),
    ('b_goods_price', 'ods_ptr_mysql_b_goods_price'),
    ('p_project_green', 'ods_ptr_mysql_p_project_green'),
    ('p_position_adjustment', 'ods_ptr_mysql_p_position_adjustment'),
    ('p_position_adjustment_item', 'ods_ptr_mysql_p_position_adjustment_item')
),
task AS (
  SELECT id, table_mapping
  FROM public.ingestion_task
  WHERE name = 'sprint25_project_datasurface'
    AND status = 'active'
  ORDER BY id DESC
  LIMIT 1
),
mapped AS (
  SELECT elem->>'source' AS source_table, elem->>'target' AS target_table
  FROM task, jsonb_array_elements(task.table_mapping) elem
)
SELECT
  COALESCE((SELECT id::text FROM task), 'missing-task') || '|' ||
  COUNT(*) FILTER (WHERE mapped.source_table IS NOT NULL)::text || '|' ||
  COALESCE(
    string_agg(required.source_table, ',' ORDER BY required.source_table)
      FILTER (WHERE mapped.source_table IS NULL),
    ''
  )
FROM required
LEFT JOIN mapped USING (source_table, target_table);
"
)

IFS='|' read -r task_id mapped_count missing_sources <<<"$task_check"
if [[ "$task_id" == "missing-task" ]]; then
  echo "[missing] active ingestion task sprint25_project_datasurface" >&2
  exit 1
fi
if [[ "$mapped_count" != "11" ]]; then
  echo "[missing] sprint25_project_datasurface maps only $mapped_count/11 tables: ${missing_sources:-unknown}" >&2
  exit 1
fi

latest_status=$(
  docker exec "$DTS_PG_CONTAINER" psql -U "$DTS_PLATFORM_USER" -d "$DTS_PLATFORM_DB" -Atc "
SELECT COALESCE((
  SELECT status
  FROM public.ingestion_execution
  WHERE task_id = $task_id
  ORDER BY id DESC
  LIMIT 1
), 'missing-execution');
"
)
if [[ "$latest_status" != "success" ]]; then
  echo "[missing] latest sprint25_project_datasurface execution is $latest_status, expected success" >&2
  exit 1
fi

row_check=$(
  docker exec "$DTS_PG_CONTAINER" psql -U "$DTS_BI_USER" -d "$DTS_BI_DB" -Atc "
WITH required(ods_table, must_have_rows) AS (
  VALUES
    ('ods_ptr_mysql_p_project', true),
    ('ods_ptr_mysql_p_customer', true),
    ('ods_ptr_mysql_p_contract', true),
    ('ods_ptr_mysql_p_position', true),
    ('ods_ptr_mysql_b_goods', true),
    ('ods_ptr_mysql_b_goods_price', true),
    ('ods_ptr_mysql_p_project_green', true),
    ('ods_ptr_mysql_p_floor_layer', false),
    ('ods_ptr_mysql_p_floor_number', false),
    ('ods_ptr_mysql_p_position_adjustment', false),
    ('ods_ptr_mysql_p_position_adjustment_item', false)
),
counts AS (
  SELECT
    table_name AS ods_table,
    (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM public.%I', table_name), false, true, '')))[1]::text::bigint AS row_count
  FROM information_schema.tables
  WHERE table_schema = 'public'
    AND table_name IN (SELECT ods_table FROM required)
)
SELECT
  COUNT(*) FILTER (WHERE counts.ods_table IS NOT NULL)::text || '|' ||
  COUNT(*) FILTER (WHERE required.must_have_rows AND COALESCE(counts.row_count, 0) > 0)::text || '|' ||
  COALESCE(
    string_agg(required.ods_table, ',' ORDER BY required.ods_table)
      FILTER (WHERE counts.ods_table IS NULL OR (required.must_have_rows AND COALESCE(counts.row_count, 0) = 0)),
    ''
  )
FROM required
LEFT JOIN counts USING (ods_table);
"
)

IFS='|' read -r found_tables non_empty_required missing_or_empty <<<"$row_check"
if [[ "$found_tables" != "11" ]]; then
  echo "[missing] only $found_tables/11 Sprint-25 ODS tables exist: ${missing_or_empty:-unknown}" >&2
  exit 1
fi
if [[ "$non_empty_required" != "7" ]]; then
  echo "[missing] only $non_empty_required/7 required Sprint-25 ODS tables have rows: ${missing_or_empty:-unknown}" >&2
  exit 1
fi

echo "[live] sprint25 project ingestion task maps 11 tables, latest execution succeeded, and required ODS tables are populated"
