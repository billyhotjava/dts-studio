#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:3003}"
ANALYTICS_USERNAME="${ANALYTICS_USERNAME:-admin}"
ANALYTICS_PASSWORD="${ANALYTICS_PASSWORD:-Devops123@}"

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

echo "[RC16-IT-01] login"
login

echo "[RC16-IT-02] fixed report catalog returns certified placeholders"
catalog_time="$(
  measure_request \
    "${tmp_dir}/catalog.json" \
    -b "${cookie_jar}" \
    "${BASE_URL}/api/report-catalog?limit=3"
)"
assert_python \
  "isinstance(payload, list) and len(payload) >= 3 and payload[0]['placeholderReviewRequired'] is True and bool(payload[0]['legacyPagePath'])" \
  "${tmp_dir}/catalog.json"
assert_under_threshold "report-catalog" "${catalog_time}" "1.000"

echo "[RC16-IT-03] fixed report run resolves template metadata instead of 404"
fixed_run_time="$(
  measure_request \
    "${tmp_dir}/fixed_run.json" \
    -b "${cookie_jar}" \
    -H 'Content-Type: application/json' \
    -d '{"parameters":{}}' \
    "${BASE_URL}/api/fixed-reports/FIN-ADVANCE-REQUEST-STATUS/run"
)"
assert_python \
  "payload['templateCode'] == 'FIN-ADVANCE-REQUEST-STATUS' and payload['executionStatus'] == 'BACKING_REQUIRED' and payload['supported'] is False and payload['legacyPagePath'] == '/operate/listAdvance'" \
  "${tmp_dir}/fixed_run.json"
assert_under_threshold "fixed-report-run" "${fixed_run_time}" "1.000"

echo "PASS test_fixed_report_fastpath"
