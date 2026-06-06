#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ZIP_FILE="$PACKAGE_ROOT/xycyl-finance-dbt-models-v1.zip"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

unzip -q "$ZIP_FILE" -d "$TMP_DIR"

MODELS_DIR="$TMP_DIR/services/dts-dbt/models"

required_files=(
  "$TMP_DIR/services/dts-dbt/macros/ensure_date_helpers.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_sale_account.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_flower_biz_info.sql"
  "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
  "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_sale_account_summary.sql"
  "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || {
    echo "[xycyl-finance-dbt-zip] missing sale model file: ${file#$TMP_DIR/}" >&2
    exit 1
  }
done

grep -q 'identifier: "ods_ptr_mysql_a_sale_account"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'identifier: "ods_ptr_mysql_t_flower_biz_info"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'name: "receivable_amount"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'name: "biz_type"' "$MODELS_DIR/xycyl_finance_sources.yml"

grep -q "ref('xycyl_stg_finance_sale_account')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "ref('xycyl_stg_finance_flower_biz_info')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "biz_type IN (5, 6, 7)" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "SUBSTRING(b.biz_code, 2, 6)" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"

grep -q 'project_id AS "projectId"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_month_settlement.sql"
grep -q 'project_id AS "projectId"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
grep -q '"应收金额"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"

unzip -p "$ZIP_FILE" models.tsv | grep -q '^xycyl_ads_sale_account_summary	ADS	'

echo "[xycyl-finance-dbt-zip] contract ok"
