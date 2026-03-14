#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "=== DTS Copilot Smoke Test ==="

# Generate certs if missing
if [[ ! -f services/certs/server.crt ]]; then
    echo "Generating certificates..."
    bash services/certs/gen-certs.sh
fi

# Start services
echo "Starting services..."
docker compose up -d
echo "Waiting for services to start (60s)..."
sleep 60

FAILED=0

check() {
    local desc="$1" cmd="$2"
    if eval "$cmd" >/dev/null 2>&1; then
        echo "  [PASS] $desc"
    else
        echo "  [FAIL] $desc"
        FAILED=1
    fi
}

echo ""
echo "=== Health Checks (Internal HTTP) ==="
check "copilot-ai health" "curl -fsS http://localhost:8091/actuator/health"
check "copilot-analytics health" "curl -fsS http://localhost:8092/actuator/health"
check "ollama tags" "curl -fsS http://localhost:11434/api/tags"

echo ""
echo "=== HTTPS Checks (via Traefik) ==="
check "HTTPS copilot-ai" "curl --cacert services/certs/ca.crt -fsS https://copilot.local/api/ai/copilot/status"
check "HTTPS webapp" "curl --cacert services/certs/ca.crt -fsS https://copilot.local/"

echo ""
echo "=== Database Schema Check ==="
check "copilot_ai schema" "docker exec dts-copilot-postgres psql -U copilot -d copilot -tAc \"SELECT schema_name FROM information_schema.schemata WHERE schema_name='copilot_ai'\" | grep -q copilot_ai"
check "copilot_analytics schema" "docker exec dts-copilot-postgres psql -U copilot -d copilot -tAc \"SELECT schema_name FROM information_schema.schemata WHERE schema_name='copilot_analytics'\" | grep -q copilot_analytics"

echo ""
if [[ $FAILED -eq 0 ]]; then
    echo "=== All smoke tests PASSED ==="
else
    echo "=== Some smoke tests FAILED ==="
fi

docker compose down
exit $FAILED
