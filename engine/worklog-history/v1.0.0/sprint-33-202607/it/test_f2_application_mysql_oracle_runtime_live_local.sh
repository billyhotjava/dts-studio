#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

MYSQL_CONTAINER="${COPILOT_FINANCE_PROOF_MYSQL_CONTAINER:-dts-copilot-finance-proof-mysql}"
MYSQL_IMAGE="${COPILOT_FINANCE_PROOF_MYSQL_IMAGE:-mysql:8.0}"
MYSQL_DATABASE="${COPILOT_FINANCE_PROOF_MYSQL_DATABASE:-rs_cloud_flower}"
MYSQL_ROOT_PASSWORD="${COPILOT_FINANCE_PROOF_MYSQL_ROOT_PASSWORD:-proof_dev_password}"
NETWORK="${DTS_CORE_NETWORK:-dts-core}"
PROOF_PG_CONTAINER="${COPILOT_FINANCE_PROOF_PG_CONTAINER:-dts-stack-dts-pg-1}"
PROOF_PG_HOST="${COPILOT_FINANCE_PROOF_PG_HOST:-dts-stack-dts-pg-1}"
PROOF_PG_PORT="${COPILOT_FINANCE_PROOF_PG_PORT:-5432}"
PROOF_PG_DATABASE="${COPILOT_FINANCE_PROOF_PG_DATABASE:-biadmin}"
PROOF_PG_USER="${COPILOT_FINANCE_PROOF_PG_USER:-biadmin}"
CASE_IDS_RAW="${COPILOT_FINANCE_PROOF_CASE_IDS:-${COPILOT_FINANCE_PROOF_CASE_ID:-voucher-year-2026-count}}"
ALL_CASE_IDS="month-settlement-discounted-receivable sale-account-receivable voucher-year-2026-count"
PG_FIELD_SEPARATOR=$'\037'

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 2
  fi
}

wait_for_mysql() {
  for _ in $(seq 1 60); do
    if docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" \
      mysql -h127.0.0.1 -P3306 -uroot -N -B -e "SELECT 1" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "temporary MySQL did not become ready" >&2
  return 1
}

wait_for_copilot_ai() {
  for _ in $(seq 1 60); do
    hstatus="$(docker inspect -f '{{.State.Health.Status}}' dts-copilot-ai 2>/dev/null || true)"
    if [ "$hstatus" = "healthy" ]; then
      return 0
    fi
    sleep 3
  done
  echo "dts-copilot-ai did not become healthy" >&2
  return 1
}

normalize_case_ids() {
  if [ "$CASE_IDS_RAW" = "all" ]; then
    printf '%s\n' "$ALL_CASE_IDS"
    return 0
  fi
  printf '%s\n' "$CASE_IDS_RAW" | tr ',' ' '
}

pg_scalar() {
  docker exec "$PROOF_PG_CONTAINER" psql \
    -U "$PROOF_PG_USER" \
    -d "$PROOF_PG_DATABASE" \
    -At \
    -v ON_ERROR_STOP=1 \
    -c "$1"
}

pg_rows() {
  docker exec "$PROOF_PG_CONTAINER" psql \
    -U "$PROOF_PG_USER" \
    -d "$PROOF_PG_DATABASE" \
    -At \
    -F "$PG_FIELD_SEPARATOR" \
    -v ON_ERROR_STOP=1 \
    -c "$1"
}

require_ads_table() {
  local table_name="$1"
  if [ "$(pg_scalar "SELECT to_regclass('public.${table_name}') IS NOT NULL;")" != "t" ]; then
    echo "required ADS table is missing: public.${table_name}" >&2
    exit 4
  fi
}

require_rows() {
  local rows="$1"
  local source_name="$2"
  if [ -z "$rows" ]; then
    echo "no 2026 proof rows found for ${source_name}" >&2
    exit 4
  fi
}

sql_string() {
  local value="${1//\'/\'\'}"
  printf "'%s'" "$value"
}

account_year() {
  printf '%s' "${1%-*}"
}

account_month() {
  printf '%s' "${1#*-}" | sed 's/^0*//'
}

restore_runtime() {
  cd "$REPO_ROOT"
  docker compose up -d copilot-ai >/dev/null 2>&1 || true
  docker rm -f "$MYSQL_CONTAINER" >/dev/null 2>&1 || true
}

require_command docker
require_command jq
trap restore_runtime EXIT

cd "$REPO_ROOT"

PROOF_PG_PASSWORD="${COPILOT_FINANCE_PROOF_PG_PASSWORD:-}"
if [ -z "$PROOF_PG_PASSWORD" ]; then
  PROOF_PG_PASSWORD="$(docker exec dts-copilot-ai sh -lc 'printf %s "$DTS_DBT_DB_PASSWORD"' 2>/dev/null || true)"
fi
if [ -z "$PROOF_PG_PASSWORD" ]; then
  echo "COPILOT_FINANCE_PROOF_PG_PASSWORD or dts-copilot-ai DTS_DBT_DB_PASSWORD is required" >&2
  exit 3
fi

CASE_IDS="$(normalize_case_ids)"
if [ -z "$CASE_IDS" ]; then
  echo "COPILOT_FINANCE_PROOF_CASE_IDS is empty" >&2
  exit 4
fi

MONTH_ROWS=""
SALE_ROWS=""
VOUCHER_ROWS=""
for case_id in $CASE_IDS; do
  case "$case_id" in
    month-settlement-discounted-receivable)
      require_ads_table "xycyl_ads_finance_month_settlement"
      MONTH_ROWS="$(pg_rows 'SELECT CAST(v."projectId" AS TEXT), v."业务月份", ROUND(SUM(v."折后实收")::numeric, 2) FROM public.xycyl_ads_finance_month_settlement v WHERE v."业务月份" LIKE '\''2026-%'\'' GROUP BY CAST(v."projectId" AS TEXT), v."业务月份" ORDER BY 1, 2;')"
      require_rows "$MONTH_ROWS" "public.xycyl_ads_finance_month_settlement"
      ;;
    sale-account-receivable)
      require_ads_table "xycyl_ads_sale_account_summary"
      SALE_ROWS="$(pg_rows 'SELECT CAST(v."projectId" AS TEXT), v."业务月份", ROUND(SUM(v."应收金额")::numeric, 2) FROM public.xycyl_ads_sale_account_summary v WHERE v."业务月份" LIKE '\''2026-%'\'' GROUP BY CAST(v."projectId" AS TEXT), v."业务月份" ORDER BY 1, 2;')"
      require_rows "$SALE_ROWS" "public.xycyl_ads_sale_account_summary"
      ;;
    voucher-year-2026-count)
      require_ads_table "xycyl_ads_finance_voucher_monthly"
      VOUCHER_ROWS="$(pg_rows 'SELECT v."会计月份", SUM(v."凭证数")::bigint FROM public.xycyl_ads_finance_voucher_monthly v WHERE v."会计月份" LIKE '\''2026-%'\'' GROUP BY v."会计月份" ORDER BY 1;')"
      require_rows "$VOUCHER_ROWS" "public.xycyl_ads_finance_voucher_monthly"
      ;;
    *)
      echo "unknown finance proof case id: ${case_id}" >&2
      exit 4
      ;;
  esac
done

docker rm -f "$MYSQL_CONTAINER" >/dev/null 2>&1 || true
docker run -d \
  --name "$MYSQL_CONTAINER" \
  --network "$NETWORK" \
  -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  -e MYSQL_DATABASE="$MYSQL_DATABASE" \
  "$MYSQL_IMAGE" \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci >/dev/null
wait_for_mysql

SEED_SQL="/tmp/codex-f2-application-mysql-proof-seed.sql"
{
  echo "USE \`$MYSQL_DATABASE\`;"
  if [ -n "$MONTH_ROWS" ]; then
    echo "CREATE TABLE a_month_accounting (id BIGINT PRIMARY KEY AUTO_INCREMENT, project_id VARCHAR(64) NOT NULL, settlement_year INT, settlement_month INT, year_and_month VARCHAR(16), status INT NOT NULL, folding_after_total_amount DECIMAL(18,4) NOT NULL);"
    while IFS="$PG_FIELD_SEPARATOR" read -r project_id account_period amount; do
      if [ -z "$account_period" ] || [ -z "$amount" ]; then
        continue
      fi
      year="$(account_year "$account_period")"
      month="$(account_month "$account_period")"
      month_padded="$(printf '%02d' "$month")"
      echo "INSERT INTO a_month_accounting (project_id, settlement_year, settlement_month, year_and_month, status, folding_after_total_amount) VALUES ($(sql_string "$project_id"), $year, $month, $(sql_string "${year}${month_padded}"), 2, $amount);"
    done <<< "$MONTH_ROWS"
  fi
  if [ -n "$SALE_ROWS" ]; then
    echo "CREATE TABLE t_flower_biz_info (id BIGINT PRIMARY KEY, project_id VARCHAR(64) NOT NULL, code VARCHAR(64) NOT NULL, biz_type INT NOT NULL);"
    echo "CREATE TABLE a_sale_account (id BIGINT PRIMARY KEY AUTO_INCREMENT, biz_id BIGINT NOT NULL, receivable_amount DECIMAL(18,4) NOT NULL);"
    biz_id=1
    while IFS="$PG_FIELD_SEPARATOR" read -r project_id account_period amount; do
      if [ -z "$account_period" ] || [ -z "$amount" ]; then
        continue
      fi
      year="$(account_year "$account_period")"
      month="$(account_month "$account_period")"
      month_padded="$(printf '%02d' "$month")"
      code="S${year}${month_padded}$(printf '%06d' "$biz_id")"
      echo "INSERT INTO t_flower_biz_info (id, project_id, code, biz_type) VALUES ($biz_id, $(sql_string "$project_id"), $(sql_string "$code"), 5);"
      echo "INSERT INTO a_sale_account (biz_id, receivable_amount) VALUES ($biz_id, $amount);"
      biz_id=$((biz_id + 1))
    done <<< "$SALE_ROWS"
  fi
  if [ -n "$VOUCHER_ROWS" ]; then
    echo "CREATE TABLE f_voucher (id BIGINT PRIMARY KEY, account_priod VARCHAR(16) NOT NULL, code VARCHAR(64));"
    echo "CREATE TABLE f_voucher_item (id BIGINT PRIMARY KEY AUTO_INCREMENT, voucher_id BIGINT NOT NULL, status INT NOT NULL);"
    voucher_id=1
    while IFS="$PG_FIELD_SEPARATOR" read -r account_period voucher_count; do
      if [ -z "$account_period" ] || [ -z "$voucher_count" ]; then
        continue
      fi
      for _ in $(seq 1 "$voucher_count"); do
        voucher_code="V$(printf '%08d' "$voucher_id")"
        echo "INSERT INTO f_voucher (id, account_priod, code) VALUES ($voucher_id, $(sql_string "$account_period"), $(sql_string "$voucher_code"));"
        echo "INSERT INTO f_voucher_item (voucher_id, status) VALUES ($voucher_id, 1);"
        voucher_id=$((voucher_id + 1))
      done
    done <<< "$VOUCHER_ROWS"
  fi
} > "$SEED_SQL"
docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" \
  mysql -h127.0.0.1 -P3306 -uroot "$MYSQL_DATABASE" < "$SEED_SQL"

if [ -n "$MONTH_ROWS" ]; then
  TOTAL_MONTH_ROWS="$(docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql \
    -h127.0.0.1 -P3306 -uroot -N -B "$MYSQL_DATABASE" \
    -e "SELECT COUNT(*) FROM a_month_accounting WHERE status > 1;")"
  if [ "${TOTAL_MONTH_ROWS:-0}" -le 0 ]; then
    echo "temporary MySQL month-settlement seed is empty" >&2
    exit 5
  fi
fi
if [ -n "$SALE_ROWS" ]; then
  TOTAL_SALE_ROWS="$(docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql \
    -h127.0.0.1 -P3306 -uroot -N -B "$MYSQL_DATABASE" \
    -e "SELECT COUNT(*) FROM a_sale_account a JOIN t_flower_biz_info b ON a.biz_id = b.id WHERE b.biz_type IN (5, 6, 7);")"
  if [ "${TOTAL_SALE_ROWS:-0}" -le 0 ]; then
    echo "temporary MySQL sale-account seed is empty" >&2
    exit 5
  fi
fi
if [ -n "$VOUCHER_ROWS" ]; then
  TOTAL_VOUCHERS="$(docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql \
    -h127.0.0.1 -P3306 -uroot -N -B "$MYSQL_DATABASE" \
    -e "SELECT COUNT(DISTINCT v.id) FROM f_voucher v WHERE v.account_priod LIKE '2026-%' AND v.code IS NOT NULL;")"
  if [ "${TOTAL_VOUCHERS:-0}" -le 0 ]; then
    echo "temporary MySQL voucher seed is empty" >&2
    exit 5
  fi
fi

COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_ENABLED=true \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_JDBC_URL="jdbc:mysql://${MYSQL_CONTAINER}:3306/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_USERNAME=root \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_PASSWORD="$MYSQL_ROOT_PASSWORD" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_DATABASE="$MYSQL_DATABASE" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_JDBC_URL="jdbc:postgresql://${PROOF_PG_HOST}:${PROOF_PG_PORT}/${PROOF_PG_DATABASE}" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_USERNAME="$PROOF_PG_USER" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_PASSWORD="$PROOF_PG_PASSWORD" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_DATABASE=prs.flowerbiz.federated \
docker compose up -d copilot-ai >/dev/null
wait_for_copilot_ai

PROVED_CASE_COUNT=0
for case_id in $CASE_IDS; do
  COPILOT_FINANCE_PROOF_CASE_ID="$case_id" \
  REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true \
  bash worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_http.sh
  PROVED_CASE_COUNT=$((PROVED_CASE_COUNT + 1))
done

echo "proved_case_count=${PROVED_CASE_COUNT}"
echo "temporary_mysql_month_settlement_rows=${TOTAL_MONTH_ROWS:-0}"
echo "temporary_mysql_sale_account_rows=${TOTAL_SALE_ROWS:-0}"
echo "temporary_mysql_voucher_count=${TOTAL_VOUCHERS:-0}"
