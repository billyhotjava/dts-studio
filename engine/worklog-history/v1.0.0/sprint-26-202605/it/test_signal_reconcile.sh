#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SQL_FILE="$ROOT_DIR/worklog/v1.0.0/sprint-26-202605/it/sql/signal_reconcile_prs_finance_cost.sql"
SCREEN_FILE="$ROOT_DIR/worklog/prs/v1/screens/prs-flowerbiz-finance-cost-v1.json"
SCREEN_CHANGELOG="$ROOT_DIR/dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0057_prs_flowerbiz_fixed_report_screens.xml"
UNIFIED_ASSET_CHANGELOG="$ROOT_DIR/dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0062_prs_flowerbiz_unified_report_assets.xml"

grep -q "PRS-FLOWERBIZ-FINANCE-COST" "$SCREEN_CHANGELOG"
grep -q "public.xycyl_ads_flowerbiz_baddebt_summary" "$SCREEN_CHANGELOG"
grep -q "public.xycyl_ads_flowerbiz_baddebt_summary" "$SCREEN_FILE"
grep -q "public.xycyl_ads_flowerbiz_finance_cost" "$SQL_FILE"
grep -q "public.xycyl_ads_flowerbiz_baddebt_summary" "$SQL_FILE"
grep -q "0.005" "$SQL_FILE"
grep -q "PRS-FLOWERBIZ-FINANCE-BADDEBT" "$UNIFIED_ASSET_CHANGELOG"

echo "[static] signal reconciliation SQL is aligned with PRS finance fixed report assets"

if [[ "${RUN_DB:-0}" == "1" ]]; then
  PG_CONTAINER="${PG_CONTAINER:-v223-dts-pg-1}"
  PG_DATABASE="${PG_DATABASE:-biadmin}"
  PG_USER="${PG_USER:-postgres}"
  output="$(docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DATABASE" -v ON_ERROR_STOP=1 -AtF '|' < "$SQL_FILE")"
  printf '%s\n' "$output"
  if printf '%s\n' "$output" | awk -F'|' 'NF && $NF != "PASS" { exit 1 }'; then
    echo "[db] signal reconciliation passed within 0.5% threshold"
  fi
fi
