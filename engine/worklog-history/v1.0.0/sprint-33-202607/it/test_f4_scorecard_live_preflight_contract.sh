#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
PREFLIGHT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f4_scorecard_live_preflight.sh"

TMP_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

cat > "$TMP_DIR/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

method="GET"
output=""
write_format=""
url=""
authorization=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    -X)
      method="$2"
      shift 2
      ;;
    -o)
      output="$2"
      shift 2
      ;;
    -w)
      write_format="$2"
      shift 2
      ;;
    -s|-S|-sS|--connect-timeout|--max-time|-H|-d)
      if [[ "$1" == "-H" && "$2" == Authorization:* ]]; then
        authorization="$2"
        shift 2
      elif [[ "$1" == "--connect-timeout" || "$1" == "--max-time" || "$1" == "-H" || "$1" == "-d" ]]; then
        shift 2
      else
        shift
      fi
      ;;
    *)
      url="$1"
      shift
      ;;
  esac
done

if [[ "$url" == *"/actuator/health"* ]]; then
  printf '%s' '{"status":"UP","components":{"financeReconciliation":{"status":"UP","details":{"scorecardId":"sprint33-finance-daily-scorecard","healthStatus":"PENDING_LIVE_EVIDENCE","requiredCategories":["f1-detail","f2-summary-voucher","f3-invariants","f4-differential-grid"]}}}}' > "$output"
  printf '200'
  exit 0
fi

if [[ "$method" == "POST" && "$url" == *"/api/auth/keys" ]]; then
  printf '%s' '{"id":42,"rawKey":"temp-scorecard-key","prefix":"sk-test"}' > "$output"
  printf '201'
  exit 0
fi

if [[ "$method" == "DELETE" && "$url" == *"/api/auth/keys/42" ]]; then
  printf '%s\n' "revoked" >> "$TMP_DIR/revoked"
  printf '%s' '{"message":"revoked"}' > "$output"
  printf '200'
  exit 0
fi

if [[ "$method" == "POST" && "$url" == *"/api/ai/finance/reconciliation-scorecards/publish-scheduled"* ]]; then
  if [[ "$authorization" == "Authorization: Bearer temp-scorecard-key" ]]; then
    if [[ "${FAKE_SCORECARD_PUBLISH_MODE:-published}" == "skipped" ]]; then
      printf '%s' '{"status":"SKIPPED","publishedCount":0,"skippedReasonCode":"NO_EVIDENCE_PROVIDER_REGISTERED","failureMessage":"Finance reconciliation scorecard schedule skipped: no finance scorecard evidence provider registered","nextAction":"enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes"}' > "$output"
    elif [[ "${FAKE_SCORECARD_PUBLISH_MODE:-published}" == "pending" ]]; then
      printf '%s' '{"status":"PENDING_LIVE_EVIDENCE","publishedCount":0,"skippedReasonCode":"PENDING_LIVE_EVIDENCE","failureMessage":"Finance reconciliation scorecard pending live evidence: scorecardId=sprint33-finance-daily-scorecard, category=f4-differential-grid, checkId=f4-live-grid","nextAction":"enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes"}' > "$output"
    else
      printf '%s' '{"status":"PUBLISHED","publishedCount":1,"failedCount":0}' > "$output"
    fi
    printf '200'
  else
    printf '%s' '{"message":"Unauthorized"}' > "$output"
    printf '401'
  fi
  exit 0
fi

printf '%s' '{"message":"not found"}' > "$output"
printf '404'
EOF

chmod +x "$TMP_DIR/curl"

OUTPUT="$(
  PATH="$TMP_DIR:$PATH" \
  TMP_DIR="$TMP_DIR" \
  COPILOT_AI_BASE_URL="http://copilot-ai:8091" \
  RUN_LIVE_F4_SCORECARD_PUBLISH=true \
  bash "$PREFLIGHT"
)"

require_line() {
  local expected="$1"
  if ! grep -Fq "$expected" <<<"$OUTPUT"; then
    echo "missing expected scorecard preflight diagnostic: $expected" >&2
    echo "$OUTPUT" >&2
    exit 1
  fi
}

require_line "status=PENDING_LIVE_EVIDENCE"
require_line "health_http_status=200"
require_line "diagnostic.finance_reconciliation_health=present"
require_line "diagnostic.finance_reconciliation_status=PENDING_LIVE_EVIDENCE"
require_line "publish_http_status=401"
require_line "reason=scorecard_publish_unauthorized"
require_line "next_action=provide FINANCE_RECONCILIATION_SCORECARD_AUTHORIZATION, COPILOT_API_KEY, or COPILOT_ADMIN_SECRET before running live scorecard publish"

OUTPUT="$(
  PATH="$TMP_DIR:$PATH" \
  TMP_DIR="$TMP_DIR" \
  COPILOT_AI_BASE_URL="http://copilot-ai:8091" \
  COPILOT_ADMIN_SECRET="admin-secret" \
  RUN_LIVE_F4_SCORECARD_PUBLISH=true \
  bash "$PREFLIGHT"
)"

require_line "diagnostic.temp_api_key_created=true"
require_line "publish_http_status=200"
require_line "diagnostic.publish_status=PUBLISHED"
require_line "diagnostic.published_count=1"
require_line "diagnostic.failed_count=0"
require_line "status=SCORECARD_PUBLISH_READY"

OUTPUT="$(
  PATH="$TMP_DIR:$PATH" \
  TMP_DIR="$TMP_DIR" \
  COPILOT_AI_BASE_URL="http://copilot-ai:8091" \
  COPILOT_ADMIN_SECRET="admin-secret" \
  FAKE_SCORECARD_PUBLISH_MODE="skipped" \
  RUN_LIVE_F4_SCORECARD_PUBLISH=true \
  bash "$PREFLIGHT"
)"

require_line "publish_http_status=200"
require_line "diagnostic.publish_status=SKIPPED"
require_line "diagnostic.skipped_reason_code=NO_EVIDENCE_PROVIDER_REGISTERED"
require_line "reason=scorecard_publish_no_evidence_provider"
require_line "next_action=enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes"

OUTPUT="$(
  PATH="$TMP_DIR:$PATH" \
  TMP_DIR="$TMP_DIR" \
  COPILOT_AI_BASE_URL="http://copilot-ai:8091" \
  COPILOT_ADMIN_SECRET="admin-secret" \
  FAKE_SCORECARD_PUBLISH_MODE="pending" \
  RUN_LIVE_F4_SCORECARD_PUBLISH=true \
  bash "$PREFLIGHT"
)"

require_line "publish_http_status=200"
require_line "diagnostic.publish_status=PENDING_LIVE_EVIDENCE"
require_line "diagnostic.skipped_reason_code=PENDING_LIVE_EVIDENCE"
require_line "diagnostic.failure_message=Finance reconciliation scorecard pending live evidence: scorecardId=sprint33-finance-daily-scorecard, category=f4-differential-grid, checkId=f4-live-grid"
require_line "reason=scorecard_publish_pending_live_evidence"
require_line "next_action=enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes"

if [[ ! -f "$TMP_DIR/revoked" ]]; then
  echo "temporary scorecard API key was not revoked" >&2
  echo "$OUTPUT" >&2
  exit 1
fi

echo "[sprint33-f4-scorecard-preflight-contract] diagnostics ok"
