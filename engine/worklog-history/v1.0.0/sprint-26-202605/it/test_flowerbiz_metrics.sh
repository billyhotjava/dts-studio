#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PACK_FILE="$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json"

jq -e '
  [.metrics[].name] == ["租金净额", "处理成本", "销售金额", "额外费用", "坏账租金损失", "项目坏账率", "客户在租金额"]
' "$PACK_FILE" >/dev/null

jq -e '
  ([.metrics[].caliber] | map(contains("dbt_amount:rent")) | any)
  and ([.metrics[].caliber] | map(contains("dbt_amount:cost")) | any)
  and ([.metrics[].caliber] | map(contains("dbt_amount:sale")) | any)
  and ([.metrics[].caliber] | map(contains("dbt_amount:extra_cost")) | any)
  and ([.metrics[].format] | contains(["currency", "percent"]))
' "$PACK_FILE" >/dev/null

echo "[static] flowerbiz tier2 metrics are present"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest test)
  echo "[junit] flowerbiz tier2 metric schema tests passed"
fi
