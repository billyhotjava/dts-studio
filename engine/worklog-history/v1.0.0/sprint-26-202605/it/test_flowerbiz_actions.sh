#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PACK_FILE="$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json"
ADMINAPI_CONTROLLER="$ROOT_DIR/../adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/flowerbiz/controller/FlowerBizInfoBadDebtController.java"
ADMINWEB_API="$ROOT_DIR/../adminweb/src/api/flower/flowerbiz/flowerBadDebt.js"

jq -e '
  (.actions | length) == 1
  and (.actions[0].name == "创建坏账处理单")
  and (.actions[0].object == "租赁报花明细")
  and (.actions[0].endpoint.service == "adminapi")
  and (.actions[0].endpoint.draft == "/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt")
  and (.actions[0].endpoint.commit == "/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt")
  and (.actions[0].approval == "human")
  and (.actions[0].audit == true)
  and (.actions[0].guard == "flowerbiz:baddebt:draft")
  and ([.actions[0].params[].name] == ["projectId", "draftItemJson", "badDebtType"])
  and (.signals[] | select(.name == "坏账风险") | (.linkedActions == ["创建坏账处理单"]))
' "$PACK_FILE" >/dev/null

grep -q '@RequestMapping("/flower/bizBadDebt")' "$ADMINAPI_CONTROLLER"
grep -q 'saveDraftFlowerBadDebt' "$ADMINAPI_CONTROLLER"
grep -q 'saveFlowerBadDebt' "$ADMINAPI_CONTROLLER"
grep -q '/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt' "$ADMINWEB_API"
grep -q '/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt' "$ADMINWEB_API"

echo "[static] flowerbiz tier3 bad-debt action is present and endpoint-aligned"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest test)
  echo "[junit] flowerbiz tier3 action schema tests passed"
fi
