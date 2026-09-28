#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PACK_FILE="$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json"

jq -e '
  [.signals[].name] == ["坏账风险", "欠费预警"]
  and [.signals[].severity] == ["high", "medium"]
  and (.signals[] | select(.name == "坏账风险") | (.when | contains("项目坏账率")) and (.linkedActions == ["创建坏账处理单"]))
  and (.signals[] | select(.name == "欠费预警") | (.when | contains("客户在租金额")))
' "$PACK_FILE" >/dev/null

echo "[static] flowerbiz tier2 signals are present"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest test)
  echo "[junit] flowerbiz tier2 signal schema tests passed"
fi
