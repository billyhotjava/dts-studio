#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
ONTOLOGY_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/OntologyService.java"
PLANNER_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AssetBackedPlannerPolicy.java"
PLAN_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ConversationPlannerService.java"

grep -q "RISK_SIGNAL_QUERY" "$PLAN_FILE"
grep -q "L2_ONTOLOGY_SIGNAL" "$PLANNER_FILE"
grep -q "resolveSignalQuery" "$PLANNER_FILE"
grep -q "buildSignalPlan" "$ONTOLOGY_FILE"
grep -q "evaluateSignals" "$ONTOLOGY_FILE"

echo "[static] signals evaluation and planner branch are present"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=OntologyServiceTest,AssetBackedPlannerPolicyTest,FlowerbizNl2SqlBaselineTest test)
  echo "[junit] signals evaluation and planner regression tests passed"
fi
