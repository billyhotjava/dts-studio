#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SPRINT_DIR="$ROOT_DIR/worklog/v1.0.0/sprint-32-202607"
STACK_DIR="${DTS_STACK_DIR:-$ROOT_DIR/../dts-stack}"

failures=()

fail() {
  failures+=("$1")
}

require_file() {
  local path="$1"
  local message="$2"
  if [[ ! -f "$path" ]]; then
    fail "$message"
  fi
}

require_no_match() {
  local pattern="$1"
  local file="$2"
  local message="$3"
  if [[ -f "$file" ]] && grep -Eq "$pattern" "$file"; then
    fail "$message"
  fi
}

require_match() {
  local pattern="$1"
  local file="$2"
  local message="$3"
  if [[ ! -f "$file" ]] || ! grep -Eq "$pattern" "$file"; then
    fail "$message"
  fi
}

require_file "$SPRINT_DIR/README.md" "missing sprint README"
require_file "$SPRINT_DIR/it/README.md" "missing sprint IT README"

require_no_match '^\| F[0-9].*\| (READY|IN_PROGRESS|BLOCKED) \|' \
  "$SPRINT_DIR/README.md" \
  "sprint README still has non-DONE feature rows"
require_no_match '^\| IT-[0-9]+.*\| (TODO|PARTIAL_PASS) \|' \
  "$SPRINT_DIR/it/README.md" \
  "IT README still has TODO or PARTIAL_PASS evidence rows"
require_no_match '^- \[ \]' \
  "$SPRINT_DIR/README.md" \
  "sprint README still has incomplete completion criteria"

compose_file="$STACK_DIR/docker-compose-app.yml"
init_file="$STACK_DIR/init.sh"
trino_config="$STACK_DIR/services/dts-trino/config.properties"
resource_groups_properties="$STACK_DIR/services/dts-trino/resource-groups.properties"
resource_groups_json="$STACK_DIR/services/dts-trino/resource-groups.json"

require_file "$compose_file" "missing dts-stack docker-compose-app.yml"
require_file "$init_file" "missing dts-stack init.sh"
require_file "$trino_config" "missing Trino config.properties"
require_file "$resource_groups_properties" "missing Trino resource-groups.properties"
require_file "$resource_groups_json" "missing Trino resource-groups.json"

require_match 'TRINO_MYSQL_READONLY_USER' "$compose_file" "Trino compose does not expose readonly MySQL user settings"
require_match 'TRINO_MYSQL_READONLY_USER:=trino_readonly' "$init_file" "init.sh does not default Trino MySQL to trino_readonly"
require_no_match 'TRINO_MYSQL_USER:=root' "$init_file" "init.sh still defaults Trino MySQL user to root"
require_match 'query\.max-run-time=5m' "$trino_config" "Trino query.max-run-time guardrail missing"
require_match 'query\.max-execution-time=3m' "$trino_config" "Trino query.max-execution-time guardrail missing"
require_match 'query\.max-scan-physical-bytes=2GB' "$trino_config" "Trino query.max-scan-physical-bytes guardrail missing"
require_match 'resource-groups.configuration-manager=file' "$resource_groups_properties" "Trino resource group manager missing"
require_match '"hardConcurrencyLimit": 4' "$resource_groups_json" "Trino resource group concurrency limit missing"
require_match '"maxQueued": 20' "$resource_groups_json" "Trino resource group queue limit missing"

if [[ -f "$compose_file" ]] && command -v docker >/dev/null 2>&1; then
  rendered_compose="$(mktemp)"
  trap 'rm -f "$rendered_compose"' EXIT
  if (cd "$STACK_DIR" && docker compose -f docker-compose-app.yml config > "$rendered_compose"); then
    if grep -Eq 'TRINO_MYSQL_USER:\s*root\s*$' "$rendered_compose"; then
      fail "rendered dts-trino MySQL user is privileged; remove high-privilege TRINO_MYSQL_* overrides and use TRINO_MYSQL_READONLY_*"
    fi
  else
    fail "docker compose config failed for dts-stack"
  fi
fi

if [[ -f "$STACK_DIR/.env" ]]; then
  if grep -Eq '^TRINO_MYSQL_USER\s*=' "$STACK_DIR/.env"; then
    fail "dts-stack .env still explicitly overrides TRINO_MYSQL_USER; use TRINO_MYSQL_READONLY_USER instead"
  fi
fi

ranger_access_file="$STACK_DIR/services/dts-trino/access-control.properties"
if [[ ! -f "$ranger_access_file" ]] || ! grep -Eiq 'ranger|access-control\.name' "$ranger_access_file"; then
  fail "Trino Ranger/access-control policy is not wired for federated catalogs"
fi
if [[ -f "$compose_file" ]] && ! grep -Eq 'access-control\.properties|ranger' "$compose_file"; then
  fail "dts-trino compose does not mount Ranger/access-control configuration"
fi

require_file \
  "$SPRINT_DIR/it/evidence/20260603-local/f4-inventory-dbt-runtime-build.md" \
  "missing inventory runtime dbt build evidence"
require_file \
  "$SPRINT_DIR/it/evidence/20260603-local/f4-inventory-ads-reconciliation.md" \
  "missing inventory ADS reconciliation evidence"

if (( ${#failures[@]} > 0 )); then
  printf '[sprint32] completion gate FAILED (%d gap(s))\n' "${#failures[@]}" >&2
  for failure in "${failures[@]}"; do
    printf -- '- %s\n' "$failure" >&2
  done
  exit 1
fi

echo "[sprint32] completion gate PASS"
