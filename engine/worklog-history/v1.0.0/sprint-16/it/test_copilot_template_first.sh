#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:3003}"
ANALYTICS_USERNAME="${ANALYTICS_USERNAME:-admin}"
ANALYTICS_PASSWORD="${ANALYTICS_PASSWORD:-Devops123@}"
DATASOURCE_ID="${DATASOURCE_ID:-7}"

tmp_dir="$(mktemp -d)"
cookie_jar="${tmp_dir}/cookies.txt"
trap 'rm -rf "${tmp_dir}"' EXIT

login() {
  curl -sS \
    -c "${cookie_jar}" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ANALYTICS_USERNAME}\",\"password\":\"${ANALYTICS_PASSWORD}\"}" \
    "${BASE_URL}/api/session" \
    > "${tmp_dir}/login.json"
}

measure_request() {
  local output_file="$1"
  shift
  curl -sS \
    -o "${output_file}" \
    -w '%{time_total}' \
    "$@"
}

assert_python() {
  local script="$1"
  local json_file="$2"
  python - <<'PY' "${json_file}" "${script}"
import json
import pathlib
import sys

json_path = pathlib.Path(sys.argv[1])
expr = sys.argv[2]
payload = json.loads(json_path.read_text())
allowed_builtins = {
    "len": len,
    "bool": bool,
    "isinstance": isinstance,
    "list": list,
    "dict": dict,
}
ns = {"payload": payload}
ok = eval(expr, {"__builtins__": allowed_builtins}, ns)
if not ok:
    raise SystemExit(
        "assertion failed: "
        + expr
        + "\npayload="
        + json.dumps(payload, ensure_ascii=False)
    )
PY
}

assert_under_threshold() {
  local label="$1"
  local value="$2"
  local threshold="$3"
  python - <<'PY' "${label}" "${value}" "${threshold}"
import sys
label, value, threshold = sys.argv[1], float(sys.argv[2]), float(sys.argv[3])
if value > threshold:
    raise SystemExit(f"{label} exceeded threshold: {value:.3f}s > {threshold:.3f}s")
print(f"{label}: {value:.3f}s")
PY
}

echo "[RC16-IT-04] login"
login

echo "[RC16-IT-05] finance question returns fixed report candidates before NL2SQL"
candidate_time="$(
  measure_request \
    "${tmp_dir}/finance_candidates.json" \
    -b "${cookie_jar}" \
    -H 'Content-Type: application/json' \
    -d "{\"userMessage\":\"财务报表\",\"datasourceId\":\"${DATASOURCE_ID}\"}" \
    "${BASE_URL}/api/copilot/chat/send"
)"
assert_python \
  "'固定报表' in payload.get('response', '') and '财务结算汇总' in payload.get('response', '') and '项目回款进度' in payload.get('response', '')" \
  "${tmp_dir}/finance_candidates.json"
assert_under_threshold "copilot-finance-candidates" "${candidate_time}" "1.000"

echo "[RC16-IT-06] purchase summary question goes to fixed report fast path"
fixed_time="$(
  measure_request \
    "${tmp_dir}/purchase_fixed.json" \
    -b "${cookie_jar}" \
    -H 'Content-Type: application/json' \
    -d "{\"userMessage\":\"采购汇总\",\"datasourceId\":\"${DATASOURCE_ID}\"}" \
    "${BASE_URL}/api/copilot/chat/send"
)"
assert_python \
  "'PROC-SUPPLIER-AMOUNT-RANK' in payload.get('response', '') and '固定报表模板' in payload.get('response', '') and '采购' in payload.get('response', '')" \
  "${tmp_dir}/purchase_fixed.json"
assert_under_threshold "copilot-purchase-fixed" "${fixed_time}" "1.000"

echo "PASS test_copilot_template_first"
