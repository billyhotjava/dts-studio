#!/usr/bin/env bash
set -euo pipefail

REQUIRE_LIVE_SCORECARD="${REQUIRE_LIVE_F4_SCORECARD:-false}"
RUN_LIVE_PUBLISH="${RUN_LIVE_F4_SCORECARD_PUBLISH:-false}"
COPILOT_AI_BASE_URL="${COPILOT_AI_BASE_URL:-${FINANCE_RECONCILIATION_SCORECARD_BASE_URL:-http://localhost:50091}}"
HEALTH_PATH="${F4_SCORECARD_HEALTH_PATH:-/actuator/health}"
PUBLISH_PATH="${F4_SCORECARD_PUBLISH_PATH:-/api/ai/finance/reconciliation-scorecards/publish-scheduled}"
AUTHORIZATION="${FINANCE_RECONCILIATION_SCORECARD_AUTHORIZATION:-${COPILOT_API_KEY:-}}"
ADMIN_SECRET="${COPILOT_ADMIN_SECRET:-}"
CONNECT_TIMEOUT="${F4_SCORECARD_CONNECT_TIMEOUT_SECONDS:-5}"
MAX_TIME="${F4_SCORECARD_MAX_TIME_SECONDS:-20}"
TEMP_KEY_ID=""

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

json_string_value() {
  local key="$1"
  local file="$2"
  grep -o "\"${key}\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" "$file" 2>/dev/null \
    | head -n 1 \
    | sed -E "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"([^\"]*)\".*/\\1/" \
    || true
}

json_number_value() {
  local key="$1"
  local file="$2"
  grep -o "\"${key}\"[[:space:]]*:[[:space:]]*[0-9][0-9]*" "$file" 2>/dev/null \
    | head -n 1 \
    | sed -E "s/.*\"${key}\"[[:space:]]*:[[:space:]]*([0-9][0-9]*).*/\\1/" \
    || true
}

pending_exit() {
  local code="$1"
  if [ "$REQUIRE_LIVE_SCORECARD" = "true" ]; then
    exit "$code"
  fi
  exit 0
}

if ! command -v curl >/dev/null 2>&1; then
  echo "status=PENDING_LIVE_EVIDENCE"
  echo "reason=missing_curl"
  pending_exit 2
fi

BASE_URL="$(trim_trailing_slash "$COPILOT_AI_BASE_URL")"
HEALTH_URL="${BASE_URL}${HEALTH_PATH}"
PUBLISH_URL="${BASE_URL}${PUBLISH_PATH}"
AUTHORIZATION_HEADER="$(normalize_authorization "$AUTHORIZATION")"

HEALTH_BODY="$(mktemp)"
PUBLISH_BODY="$(mktemp)"
cleanup() {
  if [ -n "$TEMP_KEY_ID" ] && [ -n "$ADMIN_SECRET" ]; then
    curl -sS -o /dev/null -X DELETE \
      --connect-timeout "$CONNECT_TIMEOUT" --max-time "$MAX_TIME" \
      -H "X-Admin-Secret: ${ADMIN_SECRET}" \
      "${BASE_URL}/api/auth/keys/${TEMP_KEY_ID}" >/dev/null || true
  fi
  rm -f "$HEALTH_BODY" "$PUBLISH_BODY"
}
trap cleanup EXIT

set +e
HEALTH_STATUS="$(curl -sS -o "$HEALTH_BODY" -w '%{http_code}' \
  --connect-timeout "$CONNECT_TIMEOUT" --max-time "$MAX_TIME" \
  -H 'Accept: application/json' \
  "$HEALTH_URL")"
HEALTH_EXIT=$?
set -e

echo "health_url=${HEALTH_URL}"
echo "health_http_status=${HEALTH_STATUS:-000}"
echo "health_curl_exit=${HEALTH_EXIT}"

if [ "$HEALTH_EXIT" -ne 0 ]; then
  echo "status=PENDING_LIVE_EVIDENCE"
  echo "reason=scorecard_health_unreachable"
  echo "next_action=start or expose dts-copilot-ai before running live scorecard publish"
  pending_exit 4
fi

if [[ "$HEALTH_STATUS" != 2* ]]; then
  echo "status=PENDING_LIVE_EVIDENCE"
  echo "reason=scorecard_health_not_ready"
  echo "next_action=make dts-copilot-ai actuator health return 2xx before running live scorecard publish"
  pending_exit 5
fi

if grep -Fq '"financeReconciliation"' "$HEALTH_BODY"; then
  echo "diagnostic.finance_reconciliation_health=present"
else
  echo "diagnostic.finance_reconciliation_health=missing"
fi

FINANCE_STATUS="$(json_string_value "healthStatus" "$HEALTH_BODY")"
if [ -z "$FINANCE_STATUS" ]; then
  FINANCE_STATUS="$(json_string_value "status" "$HEALTH_BODY")"
fi
if [ -n "$FINANCE_STATUS" ]; then
  echo "diagnostic.finance_reconciliation_status=${FINANCE_STATUS}"
fi

if [ "$RUN_LIVE_PUBLISH" != "true" ]; then
  echo "status=PENDING_LIVE_EVIDENCE"
  echo "reason=scorecard_publish_not_requested"
  echo "next_action=set RUN_LIVE_F4_SCORECARD_PUBLISH=true with FINANCE_RECONCILIATION_SCORECARD_AUTHORIZATION, COPILOT_API_KEY, or COPILOT_ADMIN_SECRET to execute live publish"
  pending_exit 6
fi

if [ -z "$AUTHORIZATION_HEADER" ] && [ -n "$ADMIN_SECRET" ]; then
  CREATE_BODY="$(mktemp)"
  CREATE_RESPONSE="$(mktemp)"
  printf '%s' '{"name":"sprint33-scorecard-publish-smoke","description":"temporary key for Sprint-33 scorecard live publish preflight","createdBy":"codex","expiresInDays":1}' > "$CREATE_BODY"
  set +e
  CREATE_STATUS="$(curl -sS -o "$CREATE_RESPONSE" -w '%{http_code}' \
    --connect-timeout "$CONNECT_TIMEOUT" --max-time "$MAX_TIME" \
    -X POST \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    -H "X-Admin-Secret: ${ADMIN_SECRET}" \
    -d @"$CREATE_BODY" \
    "${BASE_URL}/api/auth/keys")"
  CREATE_EXIT=$?
  set -e
  rm -f "$CREATE_BODY"

  if [ "$CREATE_EXIT" -ne 0 ] || [ "$CREATE_STATUS" != "201" ]; then
    rm -f "$CREATE_RESPONSE"
    echo "status=PENDING_LIVE_EVIDENCE"
    echo "reason=scorecard_temp_api_key_unavailable"
    echo "diagnostic.create_key_http_status=${CREATE_STATUS:-000}"
    echo "diagnostic.create_key_curl_exit=${CREATE_EXIT}"
    echo "next_action=provide a valid COPILOT_ADMIN_SECRET or pre-created COPILOT_API_KEY"
    pending_exit 8
  fi

  RAW_TEMP_KEY="$(json_string_value "rawKey" "$CREATE_RESPONSE")"
  TEMP_KEY_ID="$(json_number_value "id" "$CREATE_RESPONSE")"
  rm -f "$CREATE_RESPONSE"
  if [ -z "$RAW_TEMP_KEY" ] || [ -z "$TEMP_KEY_ID" ]; then
    echo "status=PENDING_LIVE_EVIDENCE"
    echo "reason=scorecard_temp_api_key_response_invalid"
    echo "next_action=inspect /api/auth/keys response contract"
    pending_exit 8
  fi
  AUTHORIZATION_HEADER="Bearer ${RAW_TEMP_KEY}"
  echo "diagnostic.temp_api_key_created=true"
fi

CURL_ARGS=(-sS -o "$PUBLISH_BODY" -w '%{http_code}' --connect-timeout "$CONNECT_TIMEOUT" --max-time "$MAX_TIME" -X POST -H 'Accept: application/json')
if [ -n "$AUTHORIZATION_HEADER" ]; then
  CURL_ARGS+=(-H "Authorization: ${AUTHORIZATION_HEADER}")
fi

set +e
PUBLISH_STATUS="$(curl "${CURL_ARGS[@]}" "$PUBLISH_URL")"
PUBLISH_EXIT=$?
set -e

echo "publish_url=${PUBLISH_URL}"
echo "publish_http_status=${PUBLISH_STATUS:-000}"
echo "publish_curl_exit=${PUBLISH_EXIT}"

if [ "$PUBLISH_EXIT" -ne 0 ]; then
  echo "status=PENDING_LIVE_EVIDENCE"
  echo "reason=scorecard_publish_unreachable"
  echo "next_action=start or expose dts-copilot-ai publish endpoint"
  pending_exit 7
fi

case "$PUBLISH_STATUS" in
  401|403)
    echo "status=PENDING_LIVE_EVIDENCE"
    echo "reason=scorecard_publish_unauthorized"
    echo "next_action=provide FINANCE_RECONCILIATION_SCORECARD_AUTHORIZATION, COPILOT_API_KEY, or COPILOT_ADMIN_SECRET before running live scorecard publish"
    pending_exit 8
    ;;
  2??)
    publish_status="$(json_string_value "status" "$PUBLISH_BODY")"
    published_count="$(json_number_value "publishedCount" "$PUBLISH_BODY")"
    failed_count="$(json_number_value "failedCount" "$PUBLISH_BODY")"
    skipped_reason_code="$(json_string_value "skippedReasonCode" "$PUBLISH_BODY")"
    failure_message="$(json_string_value "failureMessage" "$PUBLISH_BODY")"
    publish_next_action="$(json_string_value "nextAction" "$PUBLISH_BODY")"
    [ -n "$publish_status" ] && echo "diagnostic.publish_status=${publish_status}"
    [ -n "$published_count" ] && echo "diagnostic.published_count=${published_count}"
    [ -n "$failed_count" ] && echo "diagnostic.failed_count=${failed_count}"
    [ -n "$skipped_reason_code" ] && echo "diagnostic.skipped_reason_code=${skipped_reason_code}"
    [ -n "$failure_message" ] && echo "diagnostic.failure_message=${failure_message}"
    if [ "${published_count:-0}" -gt 0 ] && [ "${failed_count:-0}" -eq 0 ]; then
      echo "status=SCORECARD_PUBLISH_READY"
      echo "next_action=attach published scorecard evidence to F4/F5 signoff package"
      exit 0
    fi
    if [ "$publish_status" = "SKIPPED" ] && [ "$skipped_reason_code" = "NO_EVIDENCE_PROVIDER_REGISTERED" ]; then
      echo "status=PENDING_LIVE_EVIDENCE"
      echo "reason=scorecard_publish_no_evidence_provider"
      echo "next_action=${publish_next_action:-enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes}"
      pending_exit 9
    fi
    if [ "$publish_status" = "PENDING_LIVE_EVIDENCE" ] || [ "$skipped_reason_code" = "PENDING_LIVE_EVIDENCE" ]; then
      echo "status=PENDING_LIVE_EVIDENCE"
      echo "reason=scorecard_publish_pending_live_evidence"
      echo "next_action=${publish_next_action:-satisfy F1/F2/F3/F4 required lanes before publishing the finance reconciliation scorecard}"
      pending_exit 9
    fi
    echo "status=PENDING_LIVE_EVIDENCE"
    echo "reason=scorecard_publish_has_no_published_snapshot"
    echo "next_action=${publish_next_action:-enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes}"
    pending_exit 9
    ;;
  *)
    echo "status=PENDING_LIVE_EVIDENCE"
    echo "reason=scorecard_publish_not_ready"
    echo "next_action=inspect publish response and dts-copilot-ai logs"
    pending_exit 10
    ;;
esac
