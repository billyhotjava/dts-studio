#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
PREFLIGHT="$REPO_ROOT/worklog/v1.0.0/sprint-33-202607/it/test_f1_authority_route_preflight.sh"

TMP_DIR="$(mktemp -d)"
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

cat > "$TMP_DIR/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" = "ps" ]; then
  printf '%s\n' \
    "dts-stack-dts-admin-1" \
    "dts-copilot-ai" \
    "portainer"
  exit 0
fi

if [ "${1:-}" = "inspect" ]; then
  printf '%s\n' '{"traefik.http.routers.dts-admin-uiapi.rule":"Host(`biadmin.yuzhicloud.com`) && PathPrefix(`/api`)"}'
  exit 0
fi

exit 1
EOF

cat > "$TMP_DIR/ss" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' 'LISTEN 0 4096 0.0.0.0:8000 0.0.0.0:*'
EOF

chmod +x "$TMP_DIR/docker" "$TMP_DIR/ss"

OUTPUT="$(
  PATH="$TMP_DIR:$PATH" \
  env -u FINANCE_RECONCILIATION_AUTHORITY_BASE_URL \
      -u COPILOT_FINANCE_RECONCILIATION_AUTHORITY_BASE_URL \
      -u FINANCE_RECONCILIATION_ORACLE_BASE_URL \
      bash "$PREFLIGHT"
)"

require_line() {
  local expected="$1"
  if ! grep -Fq "$expected" <<<"$OUTPUT"; then
    echo "missing expected preflight diagnostic: $expected" >&2
    echo "$OUTPUT" >&2
    exit 1
  fi
}

require_line "status=PENDING_LIVE_EVIDENCE"
require_line "reason=missing_authority_base_url"
require_line "diagnostic.legacy_adminapi_containers=missing"
require_line "diagnostic.dts_admin_rs_flowers_route=missing"
require_line "diagnostic.host_port_8000=in_use"
require_line "next_action=provide FINANCE_RECONCILIATION_AUTHORITY_BASE_URL for legacy adminapi rs-gateway or /flowers-dev-api"

echo "[sprint33-f1-preflight-contract] diagnostics ok"
