#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${COPILOT_BASE_URL:-http://127.0.0.1:${COPILOT_AI_PORT:-50091}}"
CASE_ID="${COPILOT_FINANCE_PROOF_CASE_ID:-voucher-year-2026-count}"
REQUIRE_LIVE_PROOF="${REQUIRE_LIVE_APPLICATION_MYSQL_PROOF:-false}"
TEMP_KEY_ID=""

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 2
  fi
}

http_status() {
  curl -sS -o "$1" -w '%{http_code}' "$2"
}

cleanup() {
  if [ -n "$TEMP_KEY_ID" ] && [ -n "${ADMIN_SECRET:-}" ]; then
    curl -sS -o /dev/null -X DELETE \
      -H "X-Admin-Secret: ${ADMIN_SECRET}" \
      "${BASE_URL}/api/auth/keys/${TEMP_KEY_ID}" || true
  fi
}

require_command curl
require_command jq
trap cleanup EXIT

HEALTH_STATUS="$(http_status /tmp/codex-f2-proof-health.json "${BASE_URL}/actuator/health")"
if [ "$HEALTH_STATUS" != "200" ]; then
  echo "health_http_status=${HEALTH_STATUS}" >&2
  exit 3
fi

UNAUTH_STATUS="$(http_status /tmp/codex-f2-proof-unauth.json "${BASE_URL}/api/ai/finance/application-mysql-oracle/cases")"
if [ "$UNAUTH_STATUS" != "401" ]; then
  echo "unauthorized_cases_http_status=${UNAUTH_STATUS}" >&2
  exit 4
fi

RAW_KEY="${COPILOT_API_KEY:-}"
if [ -z "$RAW_KEY" ]; then
  ADMIN_SECRET="${COPILOT_ADMIN_SECRET:-}"
  if [ -z "$ADMIN_SECRET" ] && command -v docker >/dev/null 2>&1; then
    ADMIN_SECRET="$(docker exec dts-copilot-ai sh -lc 'printf %s "$COPILOT_ADMIN_SECRET"' 2>/dev/null || true)"
  fi
  if [ -z "$ADMIN_SECRET" ]; then
    echo "COPILOT_API_KEY or COPILOT_ADMIN_SECRET is required" >&2
    exit 5
  fi
  CREATE_BODY='{"name":"sprint33-finance-proof-smoke","description":"temporary key for Sprint-33 finance proof smoke","createdBy":"codex","expiresInDays":1}'
  CREATE_RESPONSE="$(curl -sS -w '\n%{http_code}' \
    -H "X-Admin-Secret: ${ADMIN_SECRET}" \
    -H 'Content-Type: application/json' \
    -d "$CREATE_BODY" \
    "${BASE_URL}/api/auth/keys")"
  CREATE_STATUS="$(printf '%s' "$CREATE_RESPONSE" | tail -n1)"
  CREATE_JSON="$(printf '%s' "$CREATE_RESPONSE" | sed '$d')"
  if [ "$CREATE_STATUS" != "201" ]; then
    echo "create_key_http_status=${CREATE_STATUS}" >&2
    exit 6
  fi
  RAW_KEY="$(printf '%s' "$CREATE_JSON" | jq -r '.rawKey // empty')"
  TEMP_KEY_ID="$(printf '%s' "$CREATE_JSON" | jq -r '.id // empty')"
  if [ -z "$RAW_KEY" ] || [ -z "$TEMP_KEY_ID" ]; then
    echo "temporary API key was not returned" >&2
    exit 7
  fi
fi

CASES_RESPONSE="$(curl -sS -w '\n%{http_code}' \
  -H "Authorization: Bearer ${RAW_KEY}" \
  "${BASE_URL}/api/ai/finance/application-mysql-oracle/cases")"
CASES_STATUS="$(printf '%s' "$CASES_RESPONSE" | tail -n1)"
CASES_JSON="$(printf '%s' "$CASES_RESPONSE" | sed '$d')"
if [ "$CASES_STATUS" != "200" ]; then
  echo "cases_http_status=${CASES_STATUS}" >&2
  exit 8
fi

CASE_COUNT="$(printf '%s' "$CASES_JSON" | jq -r '.data | length')"
if [ "$CASE_COUNT" -lt 3 ]; then
  echo "case_count=${CASE_COUNT}" >&2
  exit 9
fi
printf '%s' "$CASES_JSON" | jq -er --arg case_id "$CASE_ID" '.data[] | select(.id == $case_id)' >/dev/null

PROVE_RESPONSE="$(curl -sS -w '\n%{http_code}' \
  -H "Authorization: Bearer ${RAW_KEY}" \
  -H 'Content-Type: application/json' \
  -d "{\"caseId\":\"${CASE_ID}\"}" \
  "${BASE_URL}/api/ai/finance/application-mysql-oracle/prove")"
PROVE_STATUS="$(printf '%s' "$PROVE_RESPONSE" | tail -n1)"
PROVE_JSON="$(printf '%s' "$PROVE_RESPONSE" | sed '$d')"
RUN_STATUS="$(printf '%s' "$PROVE_JSON" | jq -r '.data.status // empty')"
RUN_MESSAGE="$(printf '%s' "$PROVE_JSON" | jq -r '.data.message // empty')"
REPORT_COUNT="$(printf '%s' "$PROVE_JSON" | jq -r '(.data.reports // []) | length')"
FIRST_FAILURE="$(printf '%s' "$PROVE_JSON" | jq -r '.data.reports[0].failureMessage // empty')"

if [ "$REQUIRE_LIVE_PROOF" = "true" ]; then
  if [ "$PROVE_STATUS" != "200" ] || [ "$RUN_STATUS" != "PASSED" ] || [ "$REPORT_COUNT" -lt 1 ]; then
    echo "prove_http_status=${PROVE_STATUS}" >&2
    echo "prove_run_status=${RUN_STATUS}" >&2
    echo "prove_report_count=${REPORT_COUNT}" >&2
    if [ -n "$RUN_MESSAGE" ]; then
      echo "prove_message=${RUN_MESSAGE}" >&2
    fi
    if [ -n "$FIRST_FAILURE" ]; then
      echo "prove_failure=${FIRST_FAILURE}" >&2
    fi
    exit 10
  fi
else
  if [ "$RUN_STATUS" != "PASSED" ] && [ "$RUN_STATUS" != "FAILED" ] && [ "$RUN_STATUS" != "DISABLED" ]; then
    echo "unexpected_prove_run_status=${RUN_STATUS}" >&2
    exit 11
  fi
fi

echo "health_http_status=${HEALTH_STATUS}"
echo "unauthorized_cases_http_status=${UNAUTH_STATUS}"
echo "cases_http_status=${CASES_STATUS}"
echo "case_count=${CASE_COUNT}"
echo "prove_http_status=${PROVE_STATUS}"
echo "prove_run_status=${RUN_STATUS}"
echo "prove_report_count=${REPORT_COUNT}"
if [ -n "$RUN_MESSAGE" ]; then
  echo "prove_message=${RUN_MESSAGE}"
fi
if [ -n "$FIRST_FAILURE" ]; then
  echo "prove_failure=${FIRST_FAILURE}"
fi
