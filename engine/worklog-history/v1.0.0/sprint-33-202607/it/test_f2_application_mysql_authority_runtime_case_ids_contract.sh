#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
FAKE_BIN="$(mktemp -d)"
PROVE_LOG="$(mktemp)"
OUTPUT_LOG="$(mktemp)"
trap 'rm -rf "$FAKE_BIN" "$PROVE_LOG" "$OUTPUT_LOG"' EXIT

cat > "$FAKE_BIN/curl" <<'FAKE_CURL'
#!/usr/bin/env bash
set -euo pipefail

out_file=""
data=""
authorization="false"
url=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    -o)
      out_file="$2"
      shift 2
      ;;
    -w)
      shift 2
      ;;
    -H)
      if [[ "$2" == Authorization:* ]]; then
        authorization="true"
      fi
      shift 2
      ;;
    -d)
      data="$2"
      shift 2
      ;;
    -X)
      shift 2
      ;;
    -*)
      shift
      ;;
    *)
      url="$1"
      shift
      ;;
  esac
done

if [ -n "$out_file" ]; then
  printf '{}' > "$out_file"
  if [[ "$url" == */actuator/health ]]; then
    printf '200'
  elif [[ "$url" == */api/ai/finance/application-mysql-authority/cases ]]; then
    printf '401'
  else
    printf '404'
  fi
  exit 0
fi

if [[ "$url" == */api/ai/finance/application-mysql-authority/cases ]] && [ "$authorization" = "true" ]; then
  printf '%s\n200' '{"data":[{"id":"month-settlement-discounted-receivable"},{"id":"sale-account-receivable"},{"id":"voucher-year-2026-count"}]}'
  exit 0
fi

if [[ "$url" == */api/ai/finance/application-mysql-authority/prove ]] && [ "$authorization" = "true" ]; then
  printf '%s\n' "$data" >> "$FAKE_CURL_PROVE_LOG"
  printf '%s\n200' '{"data":{"status":"PASSED","reports":[{"failureMessage":""}]}}'
  exit 0
fi

printf '%s\n500' '{"error":"unexpected fake curl call"}'
FAKE_CURL
chmod +x "$FAKE_BIN/curl"

cd "$REPO_ROOT"
PATH="$FAKE_BIN:$PATH" \
FAKE_CURL_PROVE_LOG="$PROVE_LOG" \
COPILOT_API_KEY="codex-test-key" \
COPILOT_BASE_URL="http://unit.test" \
COPILOT_FINANCE_PROOF_CASE_IDS="month-settlement-discounted-receivable,voucher-year-2026-count" \
REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true \
bash worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_http.sh > "$OUTPUT_LOG"

PROVE_CALLS="$(wc -l < "$PROVE_LOG" | tr -d ' ')"
if [ "$PROVE_CALLS" != "2" ]; then
  echo "expected two /prove calls for COPILOT_FINANCE_PROOF_CASE_IDS, got ${PROVE_CALLS}" >&2
  cat "$OUTPUT_LOG" >&2
  exit 4
fi

if ! rg -n 'month-settlement-discounted-receivable' "$PROVE_LOG" >/dev/null; then
  echo "month-settlement case was not proved" >&2
  exit 4
fi
if ! rg -n 'voucher-year-2026-count' "$PROVE_LOG" >/dev/null; then
  echo "voucher case was not proved" >&2
  exit 4
fi
if ! rg -n 'proved_case_count=2' "$OUTPUT_LOG" >/dev/null; then
  echo "runtime HTTP proof did not report proved_case_count=2" >&2
  cat "$OUTPUT_LOG" >&2
  exit 4
fi

if ! rg -n 'COPILOT_FINANCE_PROOF_CASE_IDS' \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_registered_datasource.sh >/dev/null; then
  echo "registered datasource proof must accept COPILOT_FINANCE_PROOF_CASE_IDS for all-case live evidence" >&2
  exit 4
fi
if rg -n 'for case_id in \$CASE_IDS' \
  worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_registered_datasource.sh >/dev/null; then
  echo "registered datasource proof must delegate all case ids to the HTTP proof once, not nest all-case loops" >&2
  exit 4
fi
