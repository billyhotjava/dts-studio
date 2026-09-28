#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODELS_DIR="$ROOT_DIR/services/dts-dbt/models"
DBT_PROJECT="$ROOT_DIR/services/dts-dbt/dbt_project.yml"
MACROS_DIR="$ROOT_DIR/services/dts-dbt/macros"

required_files=(
  "$MACROS_DIR/ensure_date_helpers.sql"
  "$MODELS_DIR/xycyl_finance_sources.yml"
  "$MODELS_DIR/xycyl_finance_stg.yml"
  "$MODELS_DIR/xycyl_finance_schema.yml"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_month_accounting.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_collection_record.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_sale_account.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_flower_biz_info.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_voucher.sql"
  "$MODELS_DIR/stg/xycyl_finance/xycyl_stg_finance_voucher_item.sql"
  "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_month_settlement.sql"
  "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_collection.sql"
  "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
  "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_voucher_ledger.sql"
  "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_monthly_summary.sql"
  "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_sale_account_summary.sql"
  "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_voucher_monthly.sql"
  "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_month_settlement.sql"
  "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_collection.sql"
  "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
  "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_voucher_monthly.sql"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || {
    echo "[xycyl-finance-dbt] missing required model file: ${file#$ROOT_DIR/}" >&2
    exit 1
  }
done

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
  if grep -A2 -E "^[[:space:]]*${model_name}:" "$DBT_PROJECT" | grep -q '+enabled: false'; then
    echo "[xycyl-finance-dbt] proof model disabled in dbt_project.yml: $model_name" >&2
    exit 1
  fi
done

grep -q 'identifier: "ods_ptr_mysql_a_month_accounting"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'identifier: "ods_ptr_mysql_a_collection_record"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'identifier: "ods_ptr_mysql_a_sale_account"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'identifier: "ods_ptr_mysql_t_flower_biz_info"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'identifier: "ods_ptr_mysql_f_voucher"' "$MODELS_DIR/xycyl_finance_sources.yml"
grep -q 'identifier: "ods_ptr_mysql_f_voucher_item"' "$MODELS_DIR/xycyl_finance_sources.yml"

grep -q "ref('xycyl_stg_finance_month_accounting')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_month_settlement.sql"
if ! grep -Eq "settlement_status[[:space:]]*>[[:space:]]*1" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_month_settlement.sql"; then
  echo "[xycyl-finance-dbt] month settlement DWD model must exclude non-effective settlement rows with settlement_status > 1" >&2
  exit 1
fi
grep -q "ref('xycyl_stg_finance_collection_record')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_collection.sql"
grep -q "ref('xycyl_stg_finance_sale_account')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "ref('xycyl_stg_finance_flower_biz_info')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "biz_type IN (5, 6, 7)" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "SUBSTRING(b.biz_code, 2, 6)" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_sale_account.sql"
grep -q "ref('xycyl_stg_finance_voucher')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_voucher_ledger.sql"
grep -q "ref('xycyl_stg_finance_voucher_item')" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_voucher_ledger.sql"
grep -q "v.voucher_id = i.voucher_id" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_voucher_ledger.sql"
! grep -q "v.voucher_code = i.voucher_code" "$MODELS_DIR/dwd/xycyl_finance/xycyl_dwd_finance_voucher_ledger.sql"

grep -q "ref('xycyl_dwd_finance_month_settlement')" "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_monthly_summary.sql"
grep -q "ref('xycyl_dwd_finance_collection')" "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_monthly_summary.sql"
grep -q "ref('xycyl_dwd_finance_sale_account')" "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_sale_account_summary.sql"
grep -q "ref('xycyl_dwd_finance_voucher_ledger')" "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_voucher_monthly.sql"
grep -q "count(DISTINCT l.voucher_id) AS voucher_count" "$MODELS_DIR/dws/xycyl_finance/xycyl_dws_finance_voucher_monthly.sql"

grep -q "ref('xycyl_dws_finance_monthly_summary')" "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_month_settlement.sql"
grep -q "ref('xycyl_dws_finance_monthly_summary')" "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_collection.sql"
grep -q "ref('xycyl_dws_finance_sale_account_summary')" "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
grep -q "ref('xycyl_dws_finance_voucher_monthly')" "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_voucher_monthly.sql"
grep -q 'project_id AS "projectId"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_finance_month_settlement.sql"
grep -q 'project_id AS "projectId"' "$MODELS_DIR/ads/xycyl_finance/xycyl_ads_sale_account_summary.sql"
if [[ "$(grep -A2 'name: "projectId"' "$MODELS_DIR/xycyl_finance_schema.yml" | grep -c 'quote: true')" -lt 2 ]]; then
  echo "[xycyl-finance-dbt] ADS projectId columns must set quote: true in schema.yml" >&2
  exit 1
fi

if grep -R "mysql.rs_cloud_flower" "$MODELS_DIR" >/dev/null; then
  echo "[xycyl-finance-dbt] dbt models must not query application MySQL directly" >&2
  exit 1
fi

echo "[xycyl-finance-dbt] contract ok"
