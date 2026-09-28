#!/usr/bin/env bash
set -euo pipefail

AI_BASE_URL="${AI_BASE_URL:-http://127.0.0.1:8091}"
COPILOT_ADMIN_SECRET="${COPILOT_ADMIN_SECRET:-change-me-in-production}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

stream_output="$tmp_dir/stream.txt"

echo "[IT-STREAM-01] verify SSE stream emits session/token/done"
curl -N -s \
  -H "X-Admin-Secret: ${COPILOT_ADMIN_SECRET}" \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"userId":"it-stream-smoke","message":"我想问下你是什么模型","datasourceId":14}' \
  "${AI_BASE_URL}/internal/agent/chat/send-stream" \
  > "${stream_output}"

grep -q '^event: session$' "${stream_output}"
grep -q '^event: token$' "${stream_output}"
grep -q '^event: done$' "${stream_output}"

echo "[IT-STREAM-02] verify stream endpoint rejects hijacked session ids"
owner_response="$(
  curl -s \
    -H "X-Admin-Secret: ${COPILOT_ADMIN_SECRET}" \
    -H 'Content-Type: application/json' \
    -d '{"userId":"it-owner-a","message":"hello"}' \
    "${AI_BASE_URL}/internal/agent/chat/send"
)"

session_id="$(printf '%s' "${owner_response}" | sed -n 's/.*"sessionId":"\([^"]*\)".*/\1/p')"
if [[ -z "${session_id}" ]]; then
  echo "failed to create owner session" >&2
  exit 1
fi

status_code="$(
  curl -s -o /dev/null -w '%{http_code}' \
    -H "X-Admin-Secret: ${COPILOT_ADMIN_SECRET}" \
    -H 'Content-Type: application/json' \
    -H 'Accept: text/event-stream' \
    -d "{\"userId\":\"it-owner-b\",\"sessionId\":\"${session_id}\",\"message\":\"steal\"}" \
    "${AI_BASE_URL}/internal/agent/chat/send-stream"
)"

if [[ "${status_code}" != "404" ]]; then
  echo "expected hijack attempt to return 404, got ${status_code}" >&2
  exit 1
fi

echo "PASS test_streaming_guardrails"
