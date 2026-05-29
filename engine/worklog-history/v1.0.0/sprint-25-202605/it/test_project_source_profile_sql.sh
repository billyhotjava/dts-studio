#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SQL_FILE="$ROOT_DIR/worklog/v1.0.0/sprint-25-202605/it/sql/project_source_profile.sql"

require_text() {
  local pattern="$1"
  if ! grep -Fq "$pattern" "$SQL_FILE"; then
    echo "missing SQL text: $pattern" >&2
    exit 1
  fi
}

reject_text() {
  local pattern="$1"
  if grep -Fq "$pattern" "$SQL_FILE"; then
    echo "forbidden SQL text: $pattern" >&2
    exit 1
  fi
}

test -f "$SQL_FILE"
require_text "to_regclass('public.ods_ptr_mysql_p_project')"
require_text "to_regclass('public.ods_ptr_mysql_p_project_green')"
require_text "to_regclass('public.ods_ptr_mysql_p_position_adjustment_item')"
require_text "_dts_source_system"
require_text "_dts_import_time"
require_text "FROM public.ods_ptr_mysql_p_project"
require_text "FROM public.ods_ptr_mysql_p_customer"
require_text "p_project status distribution"
reject_text "xycyl_ods"
reject_text "rs_cloud_flower"

if [[ "${RUN_LIVE:-0}" == "1" ]]; then
  : "${DTS_PG_CONTAINER:=v223-dts-pg-1}"
  : "${DTS_PG_USER:=biadmin}"
  : "${DTS_PG_DB:=biadmin}"
  docker exec -i "$DTS_PG_CONTAINER" psql -U "$DTS_PG_USER" -d "$DTS_PG_DB" -f - < "$SQL_FILE"
fi
