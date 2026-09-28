#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
STACK_DIR="${DTS_STACK_DIR:-$ROOT_DIR/../dts-stack}"

compose_file="$STACK_DIR/docker-compose-app.yml"
init_file="$STACK_DIR/init.sh"
mysql_catalog="$STACK_DIR/services/dts-trino/init/catalog/mysql.properties"
trino_config="$STACK_DIR/services/dts-trino/config.properties"
resource_groups_properties="$STACK_DIR/services/dts-trino/resource-groups.properties"
resource_groups_json="$STACK_DIR/services/dts-trino/resource-groups.json"

test -f "$compose_file"
test -f "$init_file"
test -f "$mysql_catalog"
test -f "$trino_config"
test -f "$resource_groups_properties"
test -f "$resource_groups_json"

grep -q 'TRINO_MYSQL_READONLY_JDBC_URL' "$compose_file"
grep -q 'TRINO_MYSQL_READONLY_USER' "$compose_file"
grep -q 'TRINO_MYSQL_READONLY_PASSWORD' "$compose_file"
grep -q 'trino_readonly' "$compose_file"
if grep -q 'TRINO_MYSQL_USER: .*root' "$compose_file"; then
  echo "[f2] forbidden root fallback in $compose_file" >&2
  exit 1
fi
grep -q 'TRINO_MYSQL_READONLY_JDBC_URL' "$init_file"
grep -q 'TRINO_MYSQL_READONLY_USER:=trino_readonly' "$init_file"
grep -q 'TRINO_MYSQL_JDBC_URL:=${TRINO_MYSQL_READONLY_JDBC_URL}' "$init_file"
grep -q 'TRINO_MYSQL_USER:=${TRINO_MYSQL_READONLY_USER}' "$init_file"
if grep -q 'TRINO_MYSQL_USER:=root' "$init_file"; then
  echo "[f2] forbidden root fallback in $init_file" >&2
  exit 1
fi

grep -q 'connection-url=${ENV:TRINO_MYSQL_JDBC_URL}' "$mysql_catalog"
grep -q 'connection-user=${ENV:TRINO_MYSQL_USER}' "$mysql_catalog"
grep -q 'connection-password=${ENV:TRINO_MYSQL_PASSWORD}' "$mysql_catalog"

grep -q 'query.max-run-time=5m' "$trino_config"
grep -q 'query.max-execution-time=3m' "$trino_config"
grep -q 'query.max-planning-time=1m' "$trino_config"
grep -q 'query.max-scan-physical-bytes=2GB' "$trino_config"
grep -q 'query.max-length=100000' "$trino_config"

grep -q 'resource-groups.configuration-manager=file' "$resource_groups_properties"
grep -q 'resource-groups.config-file=/etc/trino/resource-groups.json' "$resource_groups_properties"
grep -q '"hardConcurrencyLimit": 4' "$resource_groups_json"
grep -q '"maxQueued": 20' "$resource_groups_json"
grep -q 'source: ./services/dts-trino/resource-groups.properties' "$compose_file"
grep -q 'target: /etc/trino/resource-groups.properties' "$compose_file"
grep -q 'source: ./services/dts-trino/resource-groups.json' "$compose_file"
grep -q 'target: /etc/trino/resource-groups.json' "$compose_file"

echo "[f2] trino mysql access policy config gate PASS"
