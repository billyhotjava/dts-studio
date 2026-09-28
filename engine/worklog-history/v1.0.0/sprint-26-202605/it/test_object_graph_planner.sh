#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PLANNER_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/AssetBackedPlannerPolicy.java"
PLAN_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ConversationPlannerService.java"

grep -q "OBJECT_GRAPH_NAVIGATION" "$PLAN_FILE"
grep -q "L1_ONTOLOGY_GRAPH" "$PLANNER_FILE"
grep -q "resolveOntologyNavigation" "$PLANNER_FILE"

echo "[static] object graph planner branch is present"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest,FlowerbizNl2SqlBaselineTest test)
  echo "[junit] object graph planner regression tests passed"
fi
