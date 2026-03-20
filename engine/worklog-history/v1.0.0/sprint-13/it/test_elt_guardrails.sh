#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../../../" && pwd)"

MAVEN_ROOT="${MAVEN_ROOT:-${PROJECT_ROOT}}"
AI_BASE_URL="${AI_BASE_URL:-http://127.0.0.1:8091}"
ANALYTICS_BASE_URL="${ANALYTICS_BASE_URL:-http://127.0.0.1:8092}"
COPILOT_ADMIN_SECRET="${COPILOT_ADMIN_SECRET:-change-me-in-production}"
COPILOT_AUTH_HEADER="${COPILOT_AUTH_HEADER:-}"
ELT_HTTP_SMOKE="${ELT_HTTP_SMOKE:-auto}"

tmp_dir="$(mktemp -d)"
temp_api_key_id=""
trap 'rm -rf "${tmp_dir}"' EXIT

json_field() {
  local file="$1"
  local field="$2"
  python3 - "$file" "$field" <<'PY'
import json
import pathlib
import sys

payload = json.loads(pathlib.Path(sys.argv[1]).read_text())
value = payload.get(sys.argv[2], "")
if value is None:
    value = ""
print(value)
PY
}

cleanup_temp_key() {
  if [[ -n "${temp_api_key_id}" ]]; then
    curl -s \
      -X DELETE \
      -H "X-Admin-Secret: ${COPILOT_ADMIN_SECRET}" \
      "${AI_BASE_URL}/api/auth/keys/${temp_api_key_id}" \
      >/dev/null || true
  fi
}
trap cleanup_temp_key EXIT
trap 'cleanup_temp_key; rm -rf "${tmp_dir}"' EXIT

resolve_auth_header() {
  if [[ -n "${COPILOT_AUTH_HEADER}" ]]; then
    printf '%s' "${COPILOT_AUTH_HEADER}"
    return 0
  fi

  local create_body="${tmp_dir}/create-key.json"
  local create_status
  create_status="$(
    curl -s \
      -o "${create_body}" \
      -w '%{http_code}' \
      -H "X-Admin-Secret: ${COPILOT_ADMIN_SECRET}" \
      -H 'Content-Type: application/json' \
      -d '{"name":"sprint13-it","description":"sprint13 elt smoke","createdBy":"codex","expiresInDays":3}' \
      "${AI_BASE_URL}/api/auth/keys"
  )"

  if [[ "${create_status}" != "201" ]]; then
    return 1
  fi

  temp_api_key_id="$(json_field "${create_body}" id)"
  local raw_key
  raw_key="$(json_field "${create_body}" rawKey)"
  if [[ -z "${raw_key}" ]]; then
    return 1
  fi

  printf 'Bearer %s' "${raw_key}"
}

run_guardrail_tests() {
  echo "[IT-ELT-01] run AI planner and routing guardrails"
  mvn -f "${MAVEN_ROOT}/pom.xml" \
    -pl dts-copilot-ai \
    -Dtest=ConversationPlannerServiceTest,AssetBackedPlannerPolicyTest,AgentExecutionServiceTest,AgentChatServiceTest,InternalAgentChatResourceTest,IntentRouterServiceTest \
    test

  echo "[IT-ELT-02] run analytics ELT guardrails"
  mvn -f "${MAVEN_ROOT}/pom.xml" \
    -pl dts-copilot-analytics \
    -Dtest=FieldOperationSyncJobTest,ProjectFulfillmentSyncJobTest,EltSyncWatermarkMappingTest,EltWatermarkServiceTest,EltMonitorResourceTest \
    test
}

run_live_http_smoke() {
  local auth_header
  if ! auth_header="$(resolve_auth_header)"; then
    if [[ "${ELT_HTTP_SMOKE}" == "on" ]]; then
      echo "failed to create auth header for ELT HTTP smoke" >&2
      exit 1
    fi
    echo "[IT-ELT-03] SKIP live ELT monitor smoke: unable to acquire auth header"
    return 0
  fi

  local status_body="${tmp_dir}/elt-status.json"
  local status_code
  status_code="$(
    curl -s \
      -o "${status_body}" \
      -w '%{http_code}' \
      -H "Authorization: ${auth_header}" \
      "${ANALYTICS_BASE_URL}/api/analytics/elt/status"
  )"

  if [[ "${status_code}" != "200" ]]; then
    if grep -q 'No static resource api/analytics/elt/status' "${status_body}"; then
      if [[ "${ELT_HTTP_SMOKE}" == "on" ]]; then
        echo "ELT monitor endpoint unavailable; restart analytics with dts.elt.enabled=true" >&2
        cat "${status_body}" >&2
        exit 1
      fi
      echo "[IT-ELT-03] SKIP live ELT monitor smoke: analytics is not exposing /api/analytics/elt/status"
      return 0
    fi

    if [[ "${ELT_HTTP_SMOKE}" == "on" ]]; then
      echo "unexpected ELT status response: HTTP ${status_code}" >&2
      cat "${status_body}" >&2
      exit 1
    fi

    echo "[IT-ELT-03] SKIP live ELT monitor smoke: HTTP ${status_code}"
    return 0
  fi

  grep -q '"targetTable":"mart_project_fulfillment_daily"' "${status_body}"
  grep -q '"targetTable":"fact_field_operation_event"' "${status_body}"

  echo "[IT-ELT-04] verify known table trigger is accepted"
  local trigger_body="${tmp_dir}/elt-trigger.json"
  local trigger_code
  trigger_code="$(
    curl -s \
      -o "${trigger_body}" \
      -w '%{http_code}' \
      -X POST \
      -H "Authorization: ${auth_header}" \
      "${ANALYTICS_BASE_URL}/api/analytics/elt/trigger/mart_project_fulfillment_daily"
  )"
  if [[ "${trigger_code}" != "202" ]]; then
    echo "expected trigger endpoint to return 202, got ${trigger_code}" >&2
    cat "${trigger_body}" >&2
    exit 1
  fi

  echo "[IT-ELT-05] verify unknown table trigger is rejected"
  local bad_trigger_body="${tmp_dir}/elt-trigger-bad.json"
  local bad_trigger_code
  bad_trigger_code="$(
    curl -s \
      -o "${bad_trigger_body}" \
      -w '%{http_code}' \
      -X POST \
      -H "Authorization: ${auth_header}" \
      "${ANALYTICS_BASE_URL}/api/analytics/elt/trigger/non_existing_mart"
  )"
  if [[ "${bad_trigger_code}" != "404" ]]; then
    echo "expected unknown table trigger to return 404, got ${bad_trigger_code}" >&2
    cat "${bad_trigger_body}" >&2
    exit 1
  fi
}

run_guardrail_tests

if [[ "${ELT_HTTP_SMOKE}" != "off" ]]; then
  run_live_http_smoke
else
  echo "[IT-ELT-03] SKIP live ELT monitor smoke: ELT_HTTP_SMOKE=off"
fi

echo "PASS test_elt_guardrails"
