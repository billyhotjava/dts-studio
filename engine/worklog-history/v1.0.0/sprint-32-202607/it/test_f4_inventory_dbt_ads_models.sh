#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../../.."

MODEL_DIR="worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated/inventory/dbt/models"
SCHEMA_FILE="${MODEL_DIR}/schema.yml"

for required in \
  "stg/inventory_stg_stock_info.sql" \
  "stg/inventory_stg_goods_price.sql" \
  "dwd/inventory_dwd_stock_balance.sql" \
  "dws/inventory_dws_stock_monthly.sql" \
  "ads/inventory_ads_overview.sql" \
  "ads/inventory_ads_low_stock_alert.sql" \
  "schema.yml"; do
  test -f "${MODEL_DIR}/${required}"
done

if find "$MODEL_DIR" -type f -name '*placeholder*' | grep -q .; then
  echo "[F4] placeholder dbt model still exists" >&2
  exit 1
fi

if rg -n "placeholder|Replace this file|where 1 = 0|cast\\(null as" "$MODEL_DIR" >/dev/null; then
  echo "[F4] dbt model still contains placeholder SQL" >&2
  exit 1
fi

grep -q "inventory_stock_info_relation" "$MODEL_DIR/stg/inventory_stg_stock_info.sql"
grep -q "mysql.rs_cloud_flower.s_stock_info" "$MODEL_DIR/stg/inventory_stg_stock_info.sql"
grep -q "inventory_goods_price_relation" "$MODEL_DIR/stg/inventory_stg_goods_price.sql"
grep -q "good_price_id" "$MODEL_DIR/dwd/inventory_dwd_stock_balance.sql"
grep -q "effective_cost_amount" "$MODEL_DIR/dwd/inventory_dwd_stock_balance.sql"
grep -q "inventory_dws_stock_monthly" "$MODEL_DIR/ads/inventory_ads_overview.sql"
grep -q "库存健康状态" "$MODEL_DIR/ads/inventory_ads_overview.sql"
grep -q "库存数量 <= 2" "$MODEL_DIR/ads/inventory_ads_low_stock_alert.sql"

grep -q "inventory_ads_overview" "$SCHEMA_FILE"
grep -q "inventory_ads_low_stock_alert" "$SCHEMA_FILE"
grep -q "expected_data_type" "$SCHEMA_FILE"
grep -q "owner: warehouse-team" "$SCHEMA_FILE"
grep -q "data_layer: ADS" "$SCHEMA_FILE"

echo "[F4] inventory dbt ADS model artifacts verified"
