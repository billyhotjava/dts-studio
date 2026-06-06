#!/usr/bin/env bash
set -euo pipefail

PLATFORM_DB_CONTAINER="${COPILOT_FINANCE_PROOF_PLATFORM_DB_CONTAINER:-dts-stack-dts-pg-1}"
PLATFORM_DB_USER="${COPILOT_FINANCE_PROOF_PLATFORM_DB_USER:-postgres}"
PLATFORM_DB_NAME="${COPILOT_FINANCE_PROOF_PLATFORM_DB_NAME:-dts_platform}"
PLATFORM_INGESTION_TASK="${COPILOT_FINANCE_PROOF_PLATFORM_INGESTION_TASK:-ptr_mysql_flow}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 2
  fi
}

sql_literal() {
  local value="${1//\'/\'\'}"
  printf "'%s'" "$value"
}

psql_platform() {
  docker exec "$PLATFORM_DB_CONTAINER" psql \
    -U "$PLATFORM_DB_USER" \
    -d "$PLATFORM_DB_NAME" \
    -At \
    -F $'\t' \
    -v ON_ERROR_STOP=1 \
    -c "$1"
}

require_command docker

TASK_EXISTS="$(psql_platform "
SELECT EXISTS (
  SELECT 1
  FROM public.ingestion_task
  WHERE name = $(sql_literal "$PLATFORM_INGESTION_TASK")
);
")"
if [ "$TASK_EXISTS" != "t" ]; then
  echo "platform ingestion task not found: ${PLATFORM_INGESTION_TASK}" >&2
  exit 4
fi

MISSING_ROWS="$(psql_platform "
WITH required(source_table, target_table) AS (
  VALUES
    ('a_month_accounting', 'ods_ptr_mysql_a_month_accounting'),
    ('a_collection_record', 'ods_ptr_mysql_a_collection_record'),
    ('a_sale_account', 'ods_ptr_mysql_a_sale_account'),
    ('t_flower_biz_info', 'ods_ptr_mysql_t_flower_biz_info'),
    ('f_voucher', 'ods_ptr_mysql_f_voucher'),
    ('f_voucher_item', 'ods_ptr_mysql_f_voucher_item')
),
task AS (
  SELECT
    COALESCE(source_config::jsonb, '{}'::jsonb) AS source_config,
    COALESCE(destination_config::jsonb, '{}'::jsonb) AS destination_config,
    COALESCE(table_mapping::jsonb, '[]'::jsonb) AS table_mapping
  FROM public.ingestion_task
  WHERE name = $(sql_literal "$PLATFORM_INGESTION_TASK")
),
source_tables AS (
  SELECT regexp_replace(source_value.value, '^.*\\.', '') AS table_name
  FROM task, jsonb_array_elements_text(COALESCE(source_config->'table', '[]'::jsonb)) AS source_value(value)
),
destination_tables AS (
  SELECT regexp_replace(destination_value.value, '^.*\\.', '') AS table_name
  FROM task, jsonb_array_elements_text(COALESCE(destination_config->'table', '[]'::jsonb)) AS destination_value(value)
  UNION
  SELECT regexp_replace(connection_value.value, '^.*\\.', '') AS table_name
  FROM task,
       jsonb_array_elements(COALESCE(destination_config->'connection', '[]'::jsonb)) AS conn,
       jsonb_array_elements_text(COALESCE(conn->'table', '[]'::jsonb)) AS connection_value(value)
),
mapping AS (
  SELECT elem->>'source' AS source_table, elem->>'target' AS target_table
  FROM task, jsonb_array_elements(table_mapping) AS elem
),
checks AS (
  SELECT 'source_config.table' AS area, r.source_table, r.target_table
  FROM required r
  WHERE NOT EXISTS (
    SELECT 1 FROM source_tables s WHERE s.table_name = r.source_table
  )
  UNION ALL
  SELECT 'destination_config.table' AS area, r.source_table, r.target_table
  FROM required r
  WHERE NOT EXISTS (
    SELECT 1 FROM destination_tables d WHERE d.table_name = r.target_table
  )
  UNION ALL
  SELECT 'table_mapping' AS area, r.source_table, r.target_table
  FROM required r
  WHERE NOT EXISTS (
    SELECT 1
    FROM mapping m
    WHERE m.source_table = r.source_table
      AND m.target_table = r.target_table
  )
)
SELECT area || ': ' || source_table || ' -> ' || target_table
FROM checks
ORDER BY area, source_table;
")"

if [ -n "$MISSING_ROWS" ]; then
  echo "finance ingestion mapping is incomplete for task: ${PLATFORM_INGESTION_TASK}" >&2
  echo "$MISSING_ROWS" >&2
  exit 4
fi

echo "[sprint-33-f2] finance ingestion mapping preflight ok"
