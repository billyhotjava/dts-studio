#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../../.."

AI_BASE="${AI_BASE:-http://127.0.0.1:50091}"
API_BASE="${API_BASE:-http://127.0.0.1:50092}"
DB_ID="${DB_ID:-9}"
SESSION_ID="$(cat /proc/sys/kernel/random/uuid)"
COPILOT_ADMIN_SECRET="${COPILOT_ADMIN_SECRET:-$(docker inspect dts-copilot-ai --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^COPILOT_ADMIN_SECRET=//p' | head -n1)}"

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

call_ai() {
  local question="$1"
  curl -fsS "${AI_BASE}/internal/agent/chat/send" \
    -H "Content-Type: application/json" \
    -H "X-Admin-Secret: ${COPILOT_ADMIN_SECRET}" \
    --data-binary "$(jq -n --arg q "$question" '{userId:"sprint32-it-f4", message:$q}')"
}

run_dataset() {
  local sql="$1"
  curl -fsS "${API_BASE}/api/dataset" \
    -H "Content-Type: application/json" \
    -H "X-Metabase-Session: ${SESSION_ID}" \
    --data-binary @- <<JSON
{"database":${DB_ID},"type":"native","native":{"query":$(jq -Rn --arg q "$sql" '$q')},"cache":{"skip":true}}
JSON
}

verify_case() {
  local question="$1"
  local expected_template="$2"
  local expected_domain="$3"
  local expected_target="$4"
  local expected_relation="$5"
  local forbidden_relation="${6:-}"

  local response sql result row_count
  response="$(call_ai "$question")"

  jq -e \
    --arg template "$expected_template" \
    --arg domain "$expected_domain" \
    --arg target "$expected_target" \
    '.responseKind == "TEMPLATE_SQL"
      and .templateCode == $template
      and .routedDomain == $domain
      and .targetView == $target
      and (.generatedSql | type == "string" and length > 0)' <<<"$response" >/dev/null

  sql="$(jq -r '.generatedSql' <<<"$response")"
  grep -q "$expected_relation" <<<"$sql"
  if [[ -n "$forbidden_relation" ]]; then
    ! grep -q "$forbidden_relation" <<<"$sql"
  fi

  result="$(run_dataset "$sql")"
  row_count="$(jq -r '.data.rows | length' <<<"$result")"
  if [[ "$row_count" -le 0 ]]; then
    echo "[F4] query returned no rows: ${question}" >&2
    exit 1
  fi

  jq -c \
    --arg question "$question" \
    --arg template "$expected_template" \
    --arg domain "$expected_domain" \
    --arg target "$expected_target" \
    --argjson rowCount "$row_count" \
    '{question:$question, templateCode:$template, domain:$domain, target:$target, rowCount:$rowCount, firstRow:(.data.rows[0] // null)}' <<<"$result"
}

verify_case \
  "看下2026年各个绿植的采购情况" \
  "TPL-34" \
  "procurement" \
  "mysql.rs_cloud_flower.t_purchase_price_item" \
  "mysql.rs_cloud_flower.t_purchase_price_item" \
  "PRODUCTION"

verify_case \
  "查询2026年销售情况" \
  "TPL-52" \
  "flowerbiz" \
  "public.xycyl_ads_flowerbiz_sale_summary" \
  "public.xycyl_ads_flowerbiz_sale_summary" \
  "v_flower_biz_detail"

verify_case \
  "展示2026年库存现状" \
  "TPL-53" \
  "warehouse" \
  "mysql.rs_cloud_flower.s_stock_info" \
  "mysql.rs_cloud_flower.s_stock_info" \
  "authority.inventory.stock_overview"

verify_case \
  "低库存预警" \
  "TPL-54" \
  "warehouse" \
  "mysql.rs_cloud_flower.s_stock_info" \
  "mysql.rs_cloud_flower.s_stock_info" \
  "authority.inventory.low_stock_alert"

echo "[F4] multi-scenario isolation verified"
