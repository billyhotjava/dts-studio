#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SPRINT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
STACK_DIR="${DTS_STACK_DIR:-/opt/prod/s10/v2.2.3}"
EVIDENCE_DIR="$SCRIPT_DIR/evidence/20260601-local"
SUMMARY="$EVIDENCE_DIR/f6-trino-federated-join-summary.md"
SQL_FILE="$SCRIPT_DIR/sql/f6_trino_federated_join.sql"
COMPOSE_FILE="$STACK_DIR/docker-compose-app.yml"

mkdir -p "$EVIDENCE_DIR"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "missing required file: $path" >&2
    return 1
  fi
}

require_text() {
  local path="$1"
  local text="$2"
  grep -F "$text" "$path" >/dev/null || {
    echo "missing text '$text' in $path" >&2
    return 1
  }
}

reject_text() {
  local path="$1"
  local text="$2"
  if grep -F "$text" "$path" >/dev/null; then
    echo "forbidden text '$text' in $path" >&2
    return 1
  fi
}

POSTGRES_CATALOG="$STACK_DIR/services/dts-trino/init/catalog/postgres.properties"
MYSQL_CATALOG="$STACK_DIR/services/dts-trino/init/catalog/mysql.properties"
ANALYTICS_POM="$ROOT_DIR/dts-copilot/dts-copilot-analytics/pom.xml"
GUARDRAIL_CLASS="$ROOT_DIR/dts-copilot/dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/FederatedQueryGuardrail.java"
GUARDRAIL_TEST="$ROOT_DIR/dts-copilot/dts-copilot-analytics/src/test/java/com/yuzhi/dts/copilot/analytics/service/FederatedQueryGuardrailTest.java"
CHANGELOG="$ROOT_DIR/dts-copilot/dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0066_trino_federated_query_database.xml"
MASTER="$ROOT_DIR/dts-copilot/dts-copilot-analytics/src/main/resources/config/liquibase/master.xml"

require_file "$COMPOSE_FILE"
require_file "$POSTGRES_CATALOG"
require_file "$MYSQL_CATALOG"
require_file "$ANALYTICS_POM"
require_file "$GUARDRAIL_CLASS"
require_file "$GUARDRAIL_TEST"
require_file "$CHANGELOG"
require_file "$MASTER"
require_file "$SQL_FILE"
require_file "$SPRINT_DIR/assets/federated-query-catalog.md"

require_text "$COMPOSE_FILE" "dts-trino:"
require_text "$COMPOSE_FILE" "IMAGE_TRINO"
require_text "$COMPOSE_FILE" "docker_huahui"
require_text "$COMPOSE_FILE" "TRINO_PG_USER"
require_text "$COMPOSE_FILE" "TRINO_MYSQL_USER"
require_text "$POSTGRES_CATALOG" "connector.name=postgresql"
require_text "$POSTGRES_CATALOG" "connection-url=jdbc:postgresql://dts-pg:5432/biadmin"
require_text "$POSTGRES_CATALOG" 'connection-password=${ENV:TRINO_PG_PASSWORD}'
require_text "$MYSQL_CATALOG" "connector.name=mysql"
require_text "$MYSQL_CATALOG" "connection-url=jdbc:mysql://mysql:3306"
require_text "$MYSQL_CATALOG" 'connection-password=${ENV:TRINO_MYSQL_PASSWORD}'
require_text "$ANALYTICS_POM" "trino-jdbc"
require_text "$GUARDRAIL_CLASS" "catalog.schema.table"
require_text "$GUARDRAIL_TEST" "postgres.public.xycyl_dwd_flowerbiz_main"
require_text "$CHANGELOG" "联邦查询入口"
require_text "$MASTER" "0066_trino_federated_query_database.xml"
require_text "$SQL_FILE" "postgres.public.xycyl_dwd_flowerbiz_main"
require_text "$SQL_FILE" "mysql.rs_cloud_flower.t_flower_biz_info"
require_text "$SQL_FILE" "JOIN"
require_text "$SQL_FILE" "LIMIT 1"

reject_text "$POSTGRES_CATALOG" "secret"
reject_text "$MYSQL_CATALOG" "secret"
reject_text "$CHANGELOG" "password\":\"secret"
reject_text "$CHANGELOG" "connection-password=secret"

live_status="SKIPPED"
if [[ "${RUN_LIVE:-0}" == "1" ]]; then
  COMPOSE=(docker compose --project-directory "$STACK_DIR" -f "$COMPOSE_FILE")
  "${COMPOSE[@]}" config --services | grep -Fx "dts-trino" >/dev/null
  curl -fsS "http://127.0.0.1:${TRINO_HTTP_PORT:-18083}/v1/info" >/dev/null
  "${COMPOSE[@]}" exec -T dts-trino trino --execute "SHOW CATALOGS" | grep -E 'postgres|mysql' >/dev/null
  federated_sql="$(grep -v '^--' "$SQL_FILE" | tr '\n' ' ')"
  "${COMPOSE[@]}" exec -T dts-trino trino --execute "$federated_sql" >/dev/null
  live_status="DONE"
fi

{
  echo "# F6 Trino Federated Join Evidence"
  echo
  echo "| Check | Status |"
  echo "|-------|--------|"
  echo "| dts-trino compose service | DONE |"
  echo "| postgres/mysql catalog files | DONE |"
  echo "| no literal catalog secrets | DONE |"
  echo "| analytics Trino JDBC dependency | DONE |"
  echo "| analytics federated guardrail | DONE |"
  echo "| analytics federated seed changelog | DONE |"
  echo "| cross-catalog SQL example | DONE |"
  echo "| live Trino join | $live_status |"
} > "$SUMMARY"

echo "F6 Trino federated join passed: $SUMMARY"
