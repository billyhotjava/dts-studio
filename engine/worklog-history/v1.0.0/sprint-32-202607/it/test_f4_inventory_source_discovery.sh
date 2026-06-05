#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../../.."

API_BASE="${API_BASE:-http://127.0.0.1:50092}"
DB_ID="${DB_ID:-9}"
SESSION_ID="$(cat /proc/sys/kernel/random/uuid)"

cleanup() {
  docker exec -i dts-copilot-postgres psql -U copilot -d copilot -v ON_ERROR_STOP=1 >/dev/null 2>&1 <<SQL || true
DELETE FROM copilot_analytics.analytics_session WHERE id = '${SESSION_ID}'::uuid;
SQL
}
trap cleanup EXIT

docker exec -i dts-copilot-postgres psql -U copilot -d copilot -v ON_ERROR_STOP=1 >/dev/null <<SQL
INSERT INTO copilot_analytics.analytics_session(id, user_id, created_at, updated_at, expires_at, last_seen_at, revoked)
SELECT '${SESSION_ID}'::uuid, id, now(), now(), now() + interval '10 minutes', now(), false
FROM copilot_analytics.analytics_user
WHERE is_active = true
ORDER BY id
LIMIT 1;
SQL

run_dataset() {
  local sql="$1"
  curl -fsS "${API_BASE}/api/dataset" \
    -H "Content-Type: application/json" \
    -H "X-Metabase-Session: ${SESSION_ID}" \
    --data-binary @- <<JSON
{"database":${DB_ID},"type":"native","native":{"query":$(jq -Rn --arg q "$sql" '$q')}}
JSON
}

TABLES_SQL="SELECT table_name FROM mysql.information_schema.tables WHERE table_schema = 'rs_cloud_flower' AND table_name IN ('s_stock_info','s_stock_item','t_warehousing_info','t_warehousing_item','t_ex_warehouse_info','t_ex_warehouse_item','b_goods_price','b_goods') ORDER BY table_name"
TABLES="$(run_dataset "$TABLES_SQL" | jq -r '.data.rows[][0]' | sort | tr '\n' ' ')"

for required in \
  b_goods \
  b_goods_price \
  s_stock_info \
  s_stock_item \
  t_ex_warehouse_info \
  t_ex_warehouse_item \
  t_warehousing_info \
  t_warehousing_item; do
  grep -q "\\b${required}\\b" <<<"$TABLES"
done

STOCK_COUNT="$(run_dataset "SELECT COUNT(*) AS cnt FROM mysql.rs_cloud_flower.s_stock_info" | jq -r '.data.rows[0][0]')"
GOODS_PRICE_COUNT="$(run_dataset "SELECT COUNT(*) AS cnt FROM mysql.rs_cloud_flower.b_goods_price" | jq -r '.data.rows[0][0]')"

if [[ "$STOCK_COUNT" -le 0 || "$GOODS_PRICE_COUNT" -le 0 ]]; then
  echo "[F4] inventory source count check failed: stock=${STOCK_COUNT}, goods_price=${GOODS_PRICE_COUNT}" >&2
  exit 1
fi

jq -e '.domain == "inventory" and (.sourceTables | length >= 8)' \
  worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated/inventory/catalog-domain.json >/dev/null
jq -e '.domain == "inventory" and (.fewShots | length >= 2)' \
  worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated/inventory/semantic-packs/inventory.json >/dev/null

grep -q "WH-LOW-STOCK-ALERT" \
  worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated/inventory/routing/inventory-route-map.md

echo "[F4] inventory source discovery verified: stock=${STOCK_COUNT}, goods_price=${GOODS_PRICE_COUNT}"
