#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PACK_FILE="$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json"

jq -e '
  [.links[].name] == ["客户_项目", "项目_报花", "报花_采购", "报花_结算"]
' "$PACK_FILE" >/dev/null

jq -e '
  (.objects | map(.name) | contains(["客户", "项目", "租赁报花明细", "采购明细", "结算单"]))
  and (.links[] | select(.name == "报花_结算") | .toKey == "biz_ids_json")
  and (.links[] | select(.name == "报花_结算") | (.joinHint | contains("JSON")))
' "$PACK_FILE" >/dev/null

echo "[static] flowerbiz object graph links are present"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest,OntologyServiceTest test)
  echo "[junit] ontology object graph JOIN tests passed"
fi
