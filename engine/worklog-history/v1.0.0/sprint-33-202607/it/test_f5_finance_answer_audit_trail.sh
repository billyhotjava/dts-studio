#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

cd "$REPO_ROOT"
mvn -q -pl dts-copilot-ai -Dtest=FinanceAnswerAuditTrailServiceTest,FinanceChatAuditTrailServiceTest,FinanceReconciliationScorecardPublisherServiceTest,FinanceReconciliationScorecardSnapshotServiceTest,AgentExecutionServiceTest,AgentChatServiceTest test
pnpm --dir dts-copilot-webapp exec vitest run \
  src/components/copilot/TracePanel.test.tsx \
  src/api/modules/copilotStreamEvent.test.ts \
  src/api/aiChatCompatibility.test.ts
