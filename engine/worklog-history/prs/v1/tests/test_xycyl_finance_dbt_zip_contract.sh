#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ZIP_FILE="$PACKAGE_ROOT/xycyl-finance-dbt-models-v1.zip"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

unzip -q "$ZIP_FILE" -d "$TMP_DIR"

MODELS_DIR="$TMP_DIR/services/dts-dbt/models"
PROJECT_FILE="$TMP_DIR/services/dts-dbt/dbt_project.yml"

required_files=(
  "$TMP_DIR/services/dts-dbt/macros/ensure_date_helpers.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_sale_account.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_flower_biz_info.sql"
  "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
  "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_sale_account_summary.sql"
  "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
  "$TMP_DIR/tests/test_xycyl_finance_dbt_model_contract.sh"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || {
    echo "[xycyl-finance-dbt-zip] missing sale model file: ${file#$TMP_DIR/}" >&2
    exit 1
  }
done

if ! cmp -s "$PACKAGE_ROOT/tests/test_xycyl_finance_dbt_model_contract.sh" "$TMP_DIR/tests/test_xycyl_finance_dbt_model_contract.sh"; then
  echo "[xycyl-finance-dbt-zip] embedded model contract script is stale" >&2
  exit 1
fi

grep -q 'identifier: "ods_ptr_mysql_a_sale_account"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'identifier: "ods_ptr_mysql_t_flower_biz_info"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'name: "receivable_amount"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'name: "biz_type"' "$MODELS_DIR/xycyl_finance_sources.yml"

grep -q "ref('xycyl_stg_finance_sale_account')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "ref('xycyl_stg_finance_flower_biz_info')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "biz_type IN (5, 6, 7)" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "SUBSTRING(b.biz_code, 2, 6)" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
if ! grep -Eq "status[[:space:]]*>[[:space:]]*1" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_month_settlement.sql"; then
  echo "[xycyl-finance-dbt-zip] month settlement DWD model must exclude non-effective settlement rows with status > 1" >&2
  exit 1
fi

grep -q 'project_id AS "projectId"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_month_settlement.sql"
grep -q 'project_id AS "projectId"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
grep -q '"应收金额"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
if [[ "$(grep -A2 'name: "projectId"' "$MODELS_DIR/xycyl_finance_schema.yml" | grep -c 'quote: true')" -lt 2 ]]; then
  echo "[xycyl-finance-dbt-zip] ADS projectId columns must set quote: true in schema.yml" >&2
  exit 1
fi

for model_name in \
  xycyl_stg_finance_month_accounting \
  xycyl_stg_finance_collection_record \
  xycyl_dwd_finance_month_settlement \
  xycyl_dwd_finance_collection \
  xycyl_dws_finance_monthly_summary \
  xycyl_ads_finance_month_settlement \
  xycyl_ads_finance_collection \
  xycyl_ads_sale_account_summary \
  xycyl_ads_finance_voucher_monthly; do
  if grep -A2 -E "^[[:space:]]*${model_name}:" "$PROJECT_FILE" | grep -q '+enabled: false'; then
    echo "[xycyl-finance-dbt-zip] proof model disabled in dbt_project.yml: $model_name" >&2
    exit 1
  fi
done

unzip -p "$ZIP_FILE" models.tsv | grep -q '^xycyl_ads_sale_account_summary	ADS	'

bash "$TMP_DIR/tests/test_xycyl_finance_dbt_model_contract.sh"

echo "[xycyl-finance-dbt-zip] contract ok"
