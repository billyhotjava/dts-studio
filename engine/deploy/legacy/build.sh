#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "=== Building dts-copilot ==="
mvn clean package -DskipTests

echo "=== Building Docker images ==="
docker compose build

echo "=== Build complete ==="
