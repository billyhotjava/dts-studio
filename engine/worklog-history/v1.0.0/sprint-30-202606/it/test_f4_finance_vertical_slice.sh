#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SPRINT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
DBT_DIR="$ROOT_DIR/dts-stack/services/dts-dbt"
PACK="$ROOT_DIR/dts-copilot/dts-copilot-ai/src/main/resources/semantic-packs/finance.json"
EVIDENCE_DIR="$SCRIPT_DIR/evidence/20260601-local"
SUMMARY="$EVIDENCE_DIR/f4-finance-vertical-slice-summary.md"

MODELS=(
  "$DBT_DIR/models/stg/model/xycyl_stg_finance_month_accounting.sql"
  "$DBT_DIR/models/stg/model/xycyl_stg_finance_green_accounting.sql"
  "$DBT_DIR/models/stg/model/xycyl_stg_finance_invoice_info.sql"
  "$DBT_DIR/models/stg/model/xycyl_stg_finance_collection_record.sql"
  "$DBT_DIR/models/dwd/model/xycyl_dwd_finance_month_accounting.sql"
  "$DBT_DIR/models/dwd/model/xycyl_dwd_finance_invoice_item.sql"
  "$DBT_DIR/models/dwd/model/xycyl_dwd_finance_collection_item.sql"
  "$DBT_DIR/models/dws/model/xycyl_dws_finance_month_settlement.sql"
  "$DBT_DIR/models/ads/model/xycyl_ads_finance_month_settlement.sql"
  "$DBT_DIR/models/ads/model/xycyl_ads_finance_invoice_progress.sql"
  "$DBT_DIR/models/ads/model/xycyl_ads_finance_collection.sql"
)

ADS_MONTH="$DBT_DIR/models/ads/model/xycyl_ads_finance_month_settlement.sql"
ADS_INVOICE="$DBT_DIR/models/ads/model/xycyl_ads_finance_invoice_progress.sql"
ADS_COLLECTION="$DBT_DIR/models/ads/model/xycyl_ads_finance_collection.sql"
SCHEMA_YML="$DBT_DIR/models/xycyl_finance_schema.yml"
SOURCES_YML="$DBT_DIR/models/xycyl_finance_sources.yml"
RECON_SQL="$SCRIPT_DIR/sql/f4_finance_adminweb_reconciliation.sql"

mkdir -p "$EVIDENCE_DIR"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "missing required file: $path" >&2
    return 1
  fi
}

require_text() {
  local path="$1"
  local text="$2"
  grep -F "$text" "$path" >/dev/null || {
    echo "missing text '$text' in $path" >&2
    return 1
  }
}

for model in "${MODELS[@]}"; do
  require_file "$model"
done

require_file "$SOURCES_YML"
require_file "$SCHEMA_YML"
require_file "$RECON_SQL"
require_file "$PACK"

require_text "$ADS_MONTH" "名义租金"
require_text "$ADS_MONTH" "应收折前"
require_text "$ADS_MONTH" "折后实收"
require_text "$ADS_MONTH" "已回款"
require_text "$ADS_MONTH" "回款进度"
require_text "$ADS_MONTH" "销售摊入金额"
require_text "$ADS_MONTH" "source_type = '8'"

require_text "$ADS_INVOICE" "开票率"
require_text "$ADS_INVOICE" "税率"
require_text "$ADS_INVOICE" "开票业务类型"

require_text "$ADS_COLLECTION" "收款金额"
require_text "$ADS_COLLECTION" "是否带票"
require_text "$ADS_COLLECTION" "收款业务类型"

for table_name in \
  "public.xycyl_ads_finance_month_settlement" \
  "public.xycyl_ads_finance_invoice_progress" \
  "public.xycyl_ads_finance_collection"; do
  require_text "$PACK" "$table_name"
  require_text "$RECON_SQL" "$table_name"
done

require_text "$SCHEMA_YML" "xycyl_ads_finance_month_settlement"
require_text "$SCHEMA_YML" "xycyl_ads_finance_invoice_progress"
require_text "$SCHEMA_YML" "xycyl_ads_finance_collection"

{
  echo "# F4 Finance Vertical Slice Evidence"
  echo
  echo "- dbt model files: ${#MODELS[@]}"
  echo "- semantic pack: $PACK"
  echo "- reconciliation SQL: $RECON_SQL"
  echo
  echo "## ADS"
  echo "- public.xycyl_ads_finance_month_settlement"
  echo "- public.xycyl_ads_finance_invoice_progress"
  echo "- public.xycyl_ads_finance_collection"
} > "$SUMMARY"

echo "F4 finance vertical slice passed: $SUMMARY"
