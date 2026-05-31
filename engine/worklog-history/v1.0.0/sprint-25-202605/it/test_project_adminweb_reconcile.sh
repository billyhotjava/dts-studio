#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SPRINT_DIR="$ROOT_DIR/worklog/v1.0.0/sprint-25-202605"
PKG_DIR="$SPRINT_DIR/assets/xycyl-project-dbt-model"
SQL_FILE="$SPRINT_DIR/it/sql/project_adminweb_summary_reconcile.sql"
EVIDENCE_FILE="$SPRINT_DIR/it/evidence/20260530-local/project-adminweb-reconcile.md"
ADMINWEB_API="$ROOT_DIR/../adminweb/src/api/flower/statistics/projectSummary.js"
ADMINWEB_VIEW="$ROOT_DIR/../adminweb/src/views/flower/statistics/projectsummary/list-project-summary.vue"
ADMINAPI_CONTROLLER="$ROOT_DIR/../adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/statistics/controller/ProjectSummaryController.java"
ADMINAPI_MAPPER="$ROOT_DIR/../adminapi/rs-modules/rs-flowers-base/src/main/resources/mapper/statistics/ProjectSummaryMapper.xml"

fail() {
  echo "$1" >&2
  exit 1
}

require_file() {
  local path="$1"
  [[ -f "$path" ]] || fail "missing required file: $path"
}

require_text() {
  local path="$1"
  local pattern="$2"
  grep -Fq "$pattern" "$path" || fail "missing text in $path: $pattern"
}

require_file "$SQL_FILE"
require_file "$EVIDENCE_FILE"
require_file "$ADMINWEB_API"
require_file "$ADMINWEB_VIEW"
require_file "$ADMINAPI_CONTROLLER"
require_file "$ADMINAPI_MAPPER"

require_text "$ADMINWEB_API" "/rs-flowers-base/statistics/projectSummary/listPage"
require_text "$ADMINWEB_VIEW" "totalMonthRent"
require_text "$ADMINWEB_VIEW" "totalRealNumber"
require_text "$ADMINAPI_CONTROLLER" "@GetMapping(\"listPage\")"
require_text "$ADMINAPI_MAPPER" "SUM( p1.total_number * p1.rent )"
require_text "$ADMINAPI_MAPPER" "SUM( p1.total_number * p1.cost )"
require_text "$ADMINAPI_MAPPER" "p1.parent_id =- 1"

for model in \
  "$PKG_DIR/models/dwd/xycyl_dwd_project_green_snapshot.sql" \
  "$PKG_DIR/models/dws/xycyl_dws_project_green_monthly.sql" \
  "$PKG_DIR/models/ads/xycyl_ads_project_overview.sql"; do
  require_file "$model"
  require_text "$model" "rent_amount_adminweb"
  require_text "$model" "cost_amount_adminweb"
  require_text "$model" "real_good_number_adminweb"
done

for field in \
  "rent_amount_adminweb_sum" \
  "cost_amount_adminweb_sum" \
  "real_good_number_adminweb_sum" \
  "green_number_adminweb_sum" \
  "flowerpot_number_adminweb_sum" \
  "flowerrack_number_adminweb_sum"; do
  require_text "$PKG_DIR/models/xycyl_project_schema.yml" "$field"
  require_text "$SQL_FILE" "$field"
done

require_text "$SQL_FILE" "ProjectSummaryMapper.xml"
require_text "$SQL_FILE" "diff_pct"
require_text "$SQL_FILE" "PASS"
require_text "$EVIDENCE_FILE" "adminweb ProjectSummary listPage"
require_text "$EVIDENCE_FILE" "0.5%"
require_text "$EVIDENCE_FILE" "RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh"

echo "[static] project adminweb reconciliation assets are present"

if [[ "${RUN_LIVE:-0}" == "1" ]]; then
  : "${DTS_PG_CONTAINER:=v223-dts-pg-1}"
  : "${DTS_PG_USER:=biadmin}"
  : "${DTS_PG_DB:=biadmin}"
  docker exec -i "$DTS_PG_CONTAINER" psql -U "$DTS_PG_USER" -d "$DTS_PG_DB" -v ON_ERROR_STOP=1 -f - < "$SQL_FILE"
  echo "[live] project adminweb reconciliation passed"
fi
