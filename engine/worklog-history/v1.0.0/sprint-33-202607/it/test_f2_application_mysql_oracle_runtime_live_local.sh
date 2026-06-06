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
CASE_ID="${COPILOT_FINANCE_PROOF_CASE_ID:-voucher-year-2026-count}"

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

ADS_ROWS="$(docker exec "$PROOF_PG_CONTAINER" psql -U "$PROOF_PG_USER" -d "$PROOF_PG_DATABASE" -At -F $'\t' -c \
  'SELECT "会计月份", SUM("凭证数")::bigint FROM public.xycyl_ads_finance_voucher_monthly WHERE "会计月份" LIKE '\''2026-%'\'' GROUP BY "会计月份" ORDER BY 1;')"
if [ -z "$ADS_ROWS" ]; then
  echo "no 2026 voucher ADS rows found in public.xycyl_ads_finance_voucher_monthly" >&2
  exit 4
fi

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
  echo "CREATE TABLE f_voucher (id BIGINT PRIMARY KEY, account_priod VARCHAR(16) NOT NULL);"
  echo "CREATE TABLE f_voucher_item (id BIGINT PRIMARY KEY AUTO_INCREMENT, voucher_id BIGINT NOT NULL, status INT NOT NULL);"
  voucher_id=1
  while IFS=$'\t' read -r account_period voucher_count; do
    if [ -z "$account_period" ] || [ -z "$voucher_count" ]; then
      continue
    fi
    for _ in $(seq 1 "$voucher_count"); do
      echo "INSERT INTO f_voucher (id, account_priod) VALUES ($voucher_id, '$account_period');"
      echo "INSERT INTO f_voucher_item (voucher_id, status) VALUES ($voucher_id, 1);"
      voucher_id=$((voucher_id + 1))
    done
  done <<< "$ADS_ROWS"
} > "$SEED_SQL"
docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" \
  mysql -h127.0.0.1 -P3306 -uroot "$MYSQL_DATABASE" < "$SEED_SQL"

TOTAL_VOUCHERS="$(docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql \
  -h127.0.0.1 -P3306 -uroot -N -B "$MYSQL_DATABASE" \
  -e "SELECT COUNT(DISTINCT v.id) FROM f_voucher v JOIN f_voucher_item i ON i.voucher_id = v.id WHERE v.account_priod LIKE '2026-%' AND COALESCE(i.status, 0) > 0;")"
if [ "${TOTAL_VOUCHERS:-0}" -le 0 ]; then
  echo "temporary MySQL voucher seed is empty" >&2
  exit 5
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

COPILOT_FINANCE_PROOF_CASE_ID="$CASE_ID" \
REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true \
bash worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_http.sh

echo "temporary_mysql_voucher_count=${TOTAL_VOUCHERS}"
