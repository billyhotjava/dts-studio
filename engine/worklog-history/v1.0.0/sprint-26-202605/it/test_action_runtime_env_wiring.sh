#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/../../../.." && pwd)
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
CLIENT_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/HttpAdminApiActionClient.java"

require_text() {
  local needle=$1
  local file=$2
  if ! grep -q "$needle" "$file"; then
    echo "[missing] $needle in $file" >&2
    exit 1
  fi
}

require_text "COPILOT_ACTION_ADMINAPI_BASE_URL" "$COMPOSE_FILE"
require_text "COPILOT_ACTION_ADMINAPI_AUTHORIZATION" "$COMPOSE_FILE"
require_text "copilot.action.adminapi.base-url" "$CLIENT_FILE"
require_text "copilot.action.adminapi.authorization" "$CLIENT_FILE"

rendered=$(
  cd "$ROOT_DIR"
  COPILOT_ACTION_ADMINAPI_BASE_URL=http://adminapi.local \
  COPILOT_ACTION_ADMINAPI_AUTHORIZATION='Bearer sentinel-action-token' \
  docker compose config
)

if ! grep -q 'COPILOT_ACTION_ADMINAPI_BASE_URL: http://adminapi.local' <<<"$rendered"; then
  echo "[missing] rendered copilot-ai COPILOT_ACTION_ADMINAPI_BASE_URL" >&2
  exit 1
fi

if ! grep -q 'COPILOT_ACTION_ADMINAPI_AUTHORIZATION: Bearer sentinel-action-token' <<<"$rendered"; then
  echo "[missing] rendered copilot-ai COPILOT_ACTION_ADMINAPI_AUTHORIZATION" >&2
  exit 1
fi

echo "[static] copilot-ai adminapi action env wiring is renderable"

if [[ "${RUN_LIVE:-0}" != "1" ]]; then
  exit 0
fi

for required in COPILOT_AI_BASE_URL COPILOT_ADMIN_SECRET; do
  if [[ -z "${!required:-}" ]]; then
    echo "[missing] RUN_LIVE=1 requires $required" >&2
    exit 1
  fi
done

if [[ -z "${COPILOT_API_KEY:-}" ]]; then
  key_response=$(curl -sS --max-time 20 -X POST "$COPILOT_AI_BASE_URL/api/auth/keys" \
    -H "Content-Type: application/json" \
    -H "X-Admin-Secret: $COPILOT_ADMIN_SECRET" \
    --data '{"name":"sprint26-action-it","description":"Sprint-26 action runtime smoke","createdBy":"it","expiresInDays":1}')
  COPILOT_API_KEY=$(jq -r '.rawKey // empty' <<<"$key_response")
  if [[ -z "$COPILOT_API_KEY" ]]; then
    echo "[failed] unable to create copilot API key" >&2
    jq '{error,message}' <<<"$key_response" >&2 || true
    exit 1
  fi
fi

marker="copilot-runtime-t04-$(date +%Y%m%d%H%M%S)"
draft_json=$(jq -cn --arg marker "$marker" '[
  {
    source: "copilot-runtime-it",
    reason: "chat approve runtime verification",
    marker: $marker
  }
] | tostring')
approve_body=$(jq -cn --arg marker "$marker" --arg draft "$draft_json" '{
  sessionId: ("sess-" + $marker),
  actionId: "flowerbiz:创建坏账处理单",
  formData: {
    projectId: 0,
    draftItemJson: $draft,
    badDebtType: 1,
    remark: $marker,
    urgent: 2
  }
}')

approve_response=$(curl -sS --max-time 40 -X POST "$COPILOT_AI_BASE_URL/api/ai/agent/chat/approve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $COPILOT_API_KEY" \
  -H "X-DTS-User-Id: sprint26-action-it" \
  -H "X-DTS-User-Name: sprint26-action-it" \
  -H "X-DTS-Roles: flowerbiz:baddebt:draft" \
  --data "$approve_body")

if ! jq -e '.requiresApproval == false' <<<"$approve_response" >/dev/null; then
  echo "[failed] approve response still requires approval" >&2
  jq '{requiresApproval,response,agentMessage}' <<<"$approve_response" >&2
  exit 1
fi

if [[ "$(jq -r '(.toolCalls // []) | length' <<<"$approve_response")" -lt 1 ]]; then
  echo "[failed] approve response has no tool call result" >&2
  jq '{response,agentMessage,toolCalls}' <<<"$approve_response" >&2
  exit 1
fi

if [[ "$(jq -r '.toolCalls[0].result.success // false' <<<"$approve_response")" != "true" ]]; then
  echo "[failed] approve draft call did not succeed" >&2
  jq '{response,agentMessage,toolCalls}' <<<"$approve_response" >&2
  exit 1
fi

echo "[live] chat approve created adminapi draft through copilot-ai"
