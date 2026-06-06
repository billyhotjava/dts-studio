#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

APP_DATA_SOURCE="${COPILOT_FINANCE_PROOF_APP_DATASOURCE:-ptr_mysql}"
CASE_ID="${COPILOT_FINANCE_PROOF_CASE_ID:-voucher-year-2026-count}"
COPILOT_DB_CONTAINER="${COPILOT_FINANCE_PROOF_COPILOT_DB_CONTAINER:-dts-copilot-postgres}"
COPILOT_DB_USER="${COPILOT_FINANCE_PROOF_COPILOT_DB_USER:-copilot}"
COPILOT_DB_NAME="${COPILOT_FINANCE_PROOF_COPILOT_DB_NAME:-copilot}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 2
  fi
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

sql_literal() {
  local value="${1//\'/\'\'}"
  printf "'%s'" "$value"
}

psql_copilot() {
  docker exec "$COPILOT_DB_CONTAINER" psql \
    -U "$COPILOT_DB_USER" \
    -d "$COPILOT_DB_NAME" \
    -At \
    -F $'\t' \
    -v ON_ERROR_STOP=1 \
    -c "$1"
}

restore_runtime() {
  cd "$REPO_ROOT"
  docker compose up -d copilot-ai >/dev/null 2>&1 || true
}

require_command docker
require_command jq
trap restore_runtime EXIT

cd "$REPO_ROOT"

DATA_SOURCE_ROW="$(psql_copilot "
SELECT jdbc_url, COALESCE(username, ''), COALESCE(password, '')
FROM copilot_ai.data_source
WHERE db_type = 'mysql'
  AND status = 'ACTIVE'
  AND (id::text = $(sql_literal "$APP_DATA_SOURCE") OR name = $(sql_literal "$APP_DATA_SOURCE"))
ORDER BY CASE WHEN id::text = $(sql_literal "$APP_DATA_SOURCE") THEN 0 ELSE 1 END
LIMIT 1;
")"
if [ -z "$DATA_SOURCE_ROW" ]; then
  echo "active MySQL data source not found in copilot_ai.data_source: ${APP_DATA_SOURCE}" >&2
  exit 3
fi
IFS=$'\t' read -r APP_JDBC_URL APP_USERNAME APP_PASSWORD <<< "$DATA_SOURCE_ROW"
if [ -z "$APP_JDBC_URL" ]; then
  echo "registered MySQL data source has empty jdbc_url: ${APP_DATA_SOURCE}" >&2
  exit 3
fi
if [ -z "$APP_PASSWORD" ]; then
  echo "registered MySQL data source has empty password: ${APP_DATA_SOURCE}" >&2
  exit 3
fi

ADS_ROW="$(docker exec dts-copilot-ai sh -lc 'printf "%s\t%s\t%s\t%s\t%s" "$DTS_DBT_DB_HOST" "$DTS_DBT_DB_PORT" "$DTS_DBT_DB_NAME" "$DTS_DBT_DB_USER" "$DTS_DBT_DB_PASSWORD"' 2>/dev/null || true)"
if [ -z "$ADS_ROW" ]; then
  echo "dts-copilot-ai ADS database environment is not available" >&2
  exit 4
fi
IFS=$'\t' read -r ADS_HOST ADS_PORT ADS_DB ADS_USERNAME ADS_PASSWORD <<< "$ADS_ROW"
if [ -z "$ADS_HOST" ] || [ -z "$ADS_PORT" ] || [ -z "$ADS_DB" ] || [ -z "$ADS_USERNAME" ] || [ -z "$ADS_PASSWORD" ]; then
  echo "dts-copilot-ai ADS database environment is incomplete" >&2
  exit 4
fi

COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_ENABLED=true \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_JDBC_URL="$APP_JDBC_URL" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_USERNAME="$APP_USERNAME" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_PASSWORD="$APP_PASSWORD" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_DATABASE=rs_cloud_flower \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_JDBC_URL="jdbc:postgresql://${ADS_HOST}:${ADS_PORT}/${ADS_DB}" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_USERNAME="$ADS_USERNAME" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_PASSWORD="$ADS_PASSWORD" \
COPILOT_FINANCE_APPLICATION_MYSQL_ORACLE_COPILOT_DATABASE=prs.flowerbiz.federated \
docker compose up -d copilot-ai >/dev/null
wait_for_copilot_ai

COPILOT_FINANCE_PROOF_CASE_ID="$CASE_ID" \
REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true \
bash worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_oracle_runtime_http.sh

echo "registered_app_datasource=${APP_DATA_SOURCE}"
echo "proved_case_id=${CASE_ID}"
