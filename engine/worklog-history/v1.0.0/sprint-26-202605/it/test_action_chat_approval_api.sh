#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
RESOURCE_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AgentChatResource.java"
EXECUTOR_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/OntologyActionExecutor.java"
CLIENT_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/HttpAdminApiActionClient.java"
RESOURCE_TEST="$ROOT_DIR/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/web/rest/AgentChatActionApprovalResourceTest.java"
EXECUTOR_TEST="$ROOT_DIR/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/OntologyActionExecutorTest.java"
CLIENT_TEST="$ROOT_DIR/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/HttpAdminApiActionClientTest.java"

grep -q '@PostMapping("/approve")' "$RESOURCE_FILE"
grep -q '@PostMapping("/cancel")' "$RESOURCE_FILE"
grep -q "OntologyActionApprovalService" "$RESOURCE_FILE"
grep -q "CopilotUserContextHolder.get" "$RESOURCE_FILE"
grep -q "agentMessage" "$RESOURCE_FILE"
grep -q "requiresApproval" "$RESOURCE_FILE"
grep -q "pendingAction" "$RESOURCE_FILE"
grep -q "resolveParamValue(param.name(), param.source()" "$EXECUTOR_FILE"
grep -q "copilot.action.adminapi.base-url is required" "$CLIENT_FILE"
grep -q "approveActionDelegatesToApprovalServiceWithCurrentUserContext" "$RESOURCE_TEST"
grep -q "cancelActionReturnsChatCompatibleResponse" "$RESOURCE_TEST"
grep -q "shouldAcceptApprovedFormParamNamesAsObjectAttributes" "$EXECUTOR_TEST"
grep -q "shouldRejectDraftCallWhenAdminApiBaseUrlIsMissing" "$CLIENT_TEST"

echo "[static] chat approve/cancel API, approved form params, and adminapi base-url guard are wired"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=AgentChatActionApprovalResourceTest,OntologyActionExecutorTest,HttpAdminApiActionClientTest test)
  echo "[junit] chat approval API tests passed"
fi
