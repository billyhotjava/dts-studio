#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${FLOWER_DB_HOST:-db.weitaor.com}"
DB_PORT="${FLOWER_DB_PORT:-3306}"
DB_USER="${FLOWER_DB_USER:-flowerai}"
DB_NAME="${FLOWER_DB_NAME:-rs_cloud_flower}"
DB_PASSWORD="${FLOWER_DB_PASSWORD:-}"

if [[ -z "${DB_PASSWORD}" ]]; then
  echo "FLOWER_DB_PASSWORD is required" >&2
  exit 1
fi

run_sql() {
  local sql="$1"
  MYSQL_PWD="${DB_PASSWORD}" mysql \
    -h "${DB_HOST}" \
    -P "${DB_PORT}" \
    -u "${DB_USER}" \
    -D "${DB_NAME}" \
    -N -B \
    -e "${sql}"
}

echo "== Procurement exact regression =="
exact_totals="$(run_sql "SELECT COUNT(*) AS row_count, COALESCE(SUM(c.real_purchase_number),0) AS total_quantity, ROUND(COALESCE(SUM(c.real_purchase_number * a.parchase_price),0),2) AS total_amount FROM t_purchase_price_item a LEFT JOIN t_purchase_info b ON a.purchase_info_id = b.id LEFT JOIN t_plan_purchase_item c ON c.purchase_price_id = a.id LEFT JOIN t_flower_biz_item d ON d.id = c.flower_item_id LEFT JOIN t_flower_biz_info f ON f.id = d.flower_biz_id WHERE d.status <> -1 AND d.id IS NOT NULL AND c.status <> -1 AND a.good_name = '绿萝' AND a.purchase_time >= '2025-02-01' AND a.purchase_time < '2025-03-01';")"
echo "${exact_totals}"
[[ "${exact_totals}" == $'158\t191\t21534.50' ]] || {
  echo "Unexpected exact totals baseline: ${exact_totals}" >&2
  exit 1
}

echo "== Procurement exact by buyer =="
exact_by_buyer="$(run_sql "SELECT b.purchase_user_name, ROUND(COALESCE(SUM(c.real_purchase_number * a.parchase_price),0),2) AS purchase_amount FROM t_purchase_price_item a LEFT JOIN t_purchase_info b ON a.purchase_info_id = b.id LEFT JOIN t_plan_purchase_item c ON c.purchase_price_id = a.id LEFT JOIN t_flower_biz_item d ON d.id = c.flower_item_id LEFT JOIN t_flower_biz_info f ON f.id = d.flower_biz_id WHERE d.status <> -1 AND d.id IS NOT NULL AND c.status <> -1 AND a.good_name = '绿萝' AND a.purchase_time >= '2025-02-01' AND a.purchase_time < '2025-03-01' GROUP BY b.purchase_user_name ORDER BY purchase_amount DESC;")"
echo "${exact_by_buyer}"
[[ "${exact_by_buyer}" == $'邹顿顿\t11979.50\n王果\t9555.00' ]] || {
  echo "Unexpected by-buyer baseline: ${exact_by_buyer}" >&2
  exit 1
}

echo "== Procurement fuzzy regression =="
fuzzy_totals="$(run_sql "SELECT COUNT(*) AS row_count, COALESCE(SUM(c.real_purchase_number),0) AS total_quantity, ROUND(COALESCE(SUM(c.real_purchase_number * a.parchase_price),0),2) AS total_amount FROM t_purchase_price_item a LEFT JOIN t_purchase_info b ON a.purchase_info_id = b.id LEFT JOIN t_plan_purchase_item c ON c.purchase_price_id = a.id LEFT JOIN t_flower_biz_item d ON d.id = c.flower_item_id LEFT JOIN t_flower_biz_info f ON f.id = d.flower_biz_id WHERE d.status <> -1 AND d.id IS NOT NULL AND c.status <> -1 AND a.good_name LIKE '%绿萝%' AND a.purchase_time >= '2025-02-01' AND a.purchase_time < '2025-03-01';")"
echo "${fuzzy_totals}"
[[ "${fuzzy_totals}" == $'359\t1628\t30191.79' ]] || {
  echo "Unexpected fuzzy totals baseline: ${fuzzy_totals}" >&2
  exit 1
}

echo "Procurement regression baseline passed."
