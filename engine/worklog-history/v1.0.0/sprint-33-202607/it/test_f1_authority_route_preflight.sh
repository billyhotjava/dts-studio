#!/usr/bin/env bash
set -euo pipefail

REQUIRE_LIVE_ROUTE="${REQUIRE_LIVE_F1_AUTHORITY_ROUTE:-false}"
AUTHORITY_BASE_URL="${FINANCE_RECONCILIATION_AUTHORITY_BASE_URL:-${COPILOT_FINANCE_RECONCILIATION_AUTHORITY_BASE_URL:-${FINANCE_RECONCILIATION_ORACLE_BASE_URL:-}}}"
AUTHORITY_AUTHORIZATION="${FINANCE_RECONCILIATION_AUTHORITY_AUTHORIZATION:-${COPILOT_FINANCE_RECONCILIATION_AUTHORITY_AUTHORIZATION:-${FINANCE_RECONCILIATION_ORACLE_AUTHORIZATION:-${FINANCE_RECONCILIATION_AUTHORIZATION:-}}}}"
AUTHORITY_COOKIE="${FINANCE_RECONCILIATION_AUTHORITY_COOKIE:-${COPILOT_FINANCE_RECONCILIATION_AUTHORITY_COOKIE:-${FINANCE_RECONCILIATION_ORACLE_COOKIE:-${FINANCE_RECONCILIATION_COOKIE:-}}}}"
AUTHORITY_ROUTE_PATH="${F1_AUTHORITY_ROUTE_PATH:-/rs-flowers-base/operate/saleAccount/listSaleAccountPage?projectId=1001&bizCode=BX202606030968}"
CONNECT_TIMEOUT="${F1_AUTHORITY_ROUTE_CONNECT_TIMEOUT_SECONDS:-5}"
MAX_TIME="${F1_AUTHORITY_ROUTE_MAX_TIME_SECONDS:-15}"

trim_trailing_slash() {
  local value="$1"
  while [[ "$value" == */ ]]; do
    value="${value%/}"
  done
  printf '%s' "$value"
}

normalize_authorization() {
  local value="$1"
  if [ -z "$value" ]; then
    return 0
  fi
  case "$value" in
    Bearer\ *|bearer\ *|Basic\ *|basic\ *)
      printf '%s' "$value"
      ;;
    *)
      printf 'Bearer %s' "$value"
      ;;
  esac
}

diagnose_legacy_containers() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "diagnostic.legacy_adminapi_containers=unknown"
    return 0
  fi

  local names
  names="$(docker ps --format '{{.Names}}' 2>/dev/null || true)"
  local matches
  matches="$(printf '%s\n' "$names" | grep -E '(^|[-_])(rs-gateway|rs-flowers-base|flowerbase|gateway)($|[-_])' || true)"
  if [ -z "$matches" ]; then
    echo "diagnostic.legacy_adminapi_containers=missing"
    return 0
  fi

  echo "diagnostic.legacy_adminapi_containers=$(printf '%s\n' "$matches" | paste -sd ',' -)"
}

diagnose_dts_admin_route() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "diagnostic.dts_admin_rs_flowers_route=unknown"
    return 0
  fi

  local labels
  labels="$(docker inspect dts-stack-dts-admin-1 --format '{{json .Config.Labels}}' 2>/dev/null || true)"
  if [ -z "$labels" ]; then
    echo "diagnostic.dts_admin_rs_flowers_route=unknown"
    return 0
  fi
  if grep -Eq '/rs-flowers-base|/flowers-dev-api' <<<"$labels"; then
    echo "diagnostic.dts_admin_rs_flowers_route=published"
    return 0
  fi
  echo "diagnostic.dts_admin_rs_flowers_route=missing"
}

diagnose_host_port_8000() {
  if ! command -v ss >/dev/null 2>&1; then
    echo "diagnostic.host_port_8000=unknown"
    return 0
  fi

  if ss -ltn 2>/dev/null | grep -Eq '(^|[[:space:]])[^[:space:]]*:8000([[:space:]]|$)'; then
    echo "diagnostic.host_port_8000=in_use"
    return 0
  fi
  echo "diagnostic.host_port_8000=free"
}

print_runtime_diagnostics() {
  diagnose_legacy_containers
  diagnose_dts_admin_route
  diagnose_host_port_8000
  echo "diagnostic.legacy_compose_web_port=8000"
  echo "next_action=provide FINANCE_RECONCILIATION_AUTHORITY_BASE_URL for legacy adminapi rs-gateway or /flowers-dev-api"
}

pending() {
  local reason="$1"
  local code="$2"
  echo "status=PENDING_LIVE_EVIDENCE"
  echo "reason=${reason}"
  echo "required_config=FINANCE_RECONCILIATION_AUTHORITY_BASE_URL"
  echo "route_hint=expected legacy adminapi rs-gateway/rs-flowers-base route; use /flowers-dev-api or equivalent gateway base URL, not the dts-admin /api service"
  print_runtime_diagnostics
  if [ "$REQUIRE_LIVE_ROUTE" = "true" ]; then
    exit "$code"
  fi
  exit 0
}

if [ -z "$AUTHORITY_BASE_URL" ]; then
  pending "missing_authority_base_url" 3
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "missing required command: curl" >&2
  exit 2
fi

BODY_FILE="$(mktemp)"
cleanup() {
  rm -f "$BODY_FILE"
}
trap cleanup EXIT

AUTHORITY_URL="$(trim_trailing_slash "$AUTHORITY_BASE_URL")${AUTHORITY_ROUTE_PATH}"
AUTHORIZATION_HEADER="$(normalize_authorization "$AUTHORITY_AUTHORIZATION")"

CURL_ARGS=(-sS -o "$BODY_FILE" -w '%{http_code}' --connect-timeout "$CONNECT_TIMEOUT" --max-time "$MAX_TIME" -H 'Accept: application/json')
if [ -n "$AUTHORIZATION_HEADER" ]; then
  CURL_ARGS+=(-H "Authorization: ${AUTHORIZATION_HEADER}")
fi
if [ -n "$AUTHORITY_COOKIE" ]; then
  CURL_ARGS+=(-H "Cookie: ${AUTHORITY_COOKIE}")
fi

set +e
HTTP_STATUS="$(curl "${CURL_ARGS[@]}" "$AUTHORITY_URL")"
CURL_EXIT=$?
set -e

echo "authority_url=${AUTHORITY_URL}"
echo "authority_http_status=${HTTP_STATUS:-000}"
echo "curl_exit=${CURL_EXIT}"

if [ "$CURL_EXIT" -ne 0 ]; then
  pending "authority_route_unreachable" 4
fi

case "$HTTP_STATUS" in
  2??)
    echo "status=ROUTE_READY"
    echo "route_hint=authority route is reachable; next gate is live L2 payload vs /api/dataset reconciliation"
    ;;
  *)
    pending "authority_route_not_ready" 5
    ;;
esac
