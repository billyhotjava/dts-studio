#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
APPROVAL_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/OntologyActionApprovalService.java"
GUARD_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/ActionGuardService.java"
AUDIT_FILE="$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/audit/AiAuditService.java"
TEST_FILE="$ROOT_DIR/dts-copilot-ai/src/test/java/com/yuzhi/dts/copilot/ai/service/copilot/OntologyActionApprovalServiceTest.java"

grep -q "requiresApproval" "$APPROVAL_FILE"
grep -q "需要用户确认" "$APPROVAL_FILE"
grep -q "createDraft" "$APPROVAL_FILE"
grep -q "logActionExecution" "$APPROVAL_FILE"
grep -q "CopilotUserContext" "$GUARD_FILE"
grep -q "缺少权限" "$GUARD_FILE"
grep -q "ACTION_EXECUTION" "$AUDIT_FILE"
grep -q "objectName" "$AUDIT_FILE"
grep -q "guard" "$AUDIT_FILE"
grep -q "never()).createDraft" "$TEST_FILE"
grep -q "shouldRejectConfirmedDraftWhenGuardIsMissing" "$TEST_FILE"
grep -q "shouldExecuteDraftAndAuditAfterConfirmationAndGuardPass" "$TEST_FILE"

echo "[static] action approval guard and audit chain is wired"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=OntologyActionApprovalServiceTest,OntologyActionExecutorTest,SemanticPackOntologySchemaTest test)
  echo "[junit] action guard/audit tests passed"
fi
