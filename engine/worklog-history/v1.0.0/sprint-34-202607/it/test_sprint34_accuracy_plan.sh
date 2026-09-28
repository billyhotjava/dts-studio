#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SPRINT_DIR="$ROOT_DIR/worklog/v1.0.0/sprint-34-202607"

required_files=(
  "$SPRINT_DIR/README.md"
  "$SPRINT_DIR/assets/nl2sql-accuracy-evidence-contract.v1.md"
  "$SPRINT_DIR/features/F1-准确性证据契约与评分/README.md"
  "$SPRINT_DIR/features/F2-路由数据层与SQL护栏/README.md"
  "$SPRINT_DIR/features/F3-数据新鲜度血缘与对账执行/README.md"
  "$SPRINT_DIR/features/F4-黄金集形变与持续评估/README.md"
  "$SPRINT_DIR/features/F5-可信解释与低可信交互/README.md"
  "$SPRINT_DIR/it/README.md"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || {
    echo "[sprint34-accuracy] missing file: ${file#$ROOT_DIR/}" >&2
    exit 1
  }
done

grep -q "accuracyEvidence" "$SPRINT_DIR/assets/nl2sql-accuracy-evidence-contract.v1.md"
grep -q "HIGH" "$SPRINT_DIR/README.md"
grep -q "MEDIUM" "$SPRINT_DIR/README.md"
grep -q "LOW" "$SPRINT_DIR/README.md"
grep -q "UNTRUSTED" "$SPRINT_DIR/README.md"
grep -q "L0 profile" "$SPRINT_DIR/README.md"
grep -q "ADS/DWS" "$SPRINT_DIR/features/F2-路由数据层与SQL护栏/README.md"
grep -q "tie-out" "$SPRINT_DIR/features/F3-数据新鲜度血缘与对账执行/README.md"
grep -q "FRESH" "$SPRINT_DIR/features/F3-数据新鲜度血缘与对账执行/T01-入湖与dbt新鲜度探针.md"
grep -q "MISSING" "$SPRINT_DIR/features/F3-数据新鲜度血缘与对账执行/T01-入湖与dbt新鲜度探针.md"
grep -q "STALE" "$SPRINT_DIR/features/F3-数据新鲜度血缘与对账执行/T01-入湖与dbt新鲜度探针.md"
grep -q "live dts-platform/dbt diagnostics" "$SPRINT_DIR/features/F3-数据新鲜度血缘与对账执行/T01-入湖与dbt新鲜度探针.md"
grep -q "2026年凭证的数据统计下" "$SPRINT_DIR/it/README.md"
grep -q "TPL-FLOWERBIZ-ORDER-MONTHLY" "$SPRINT_DIR/it/README.md"
grep -R -q "class Nl2SqlAccuracyEvidenceContractTest" "$ROOT_DIR/dts-copilot-ai/src/test/java"
grep -R -q "class Nl2SqlAccuracyGoldenSetRegistryTest" "$ROOT_DIR/dts-copilot-ai/src/test/java"
grep -R -q "class Nl2SqlAccuracyGoldenSetReleaseEvidenceServiceTest" "$ROOT_DIR/dts-copilot-ai/src/test/java"
grep -R -q "class CopilotChatRequestContextTest" "$ROOT_DIR/dts-copilot-ai/src/test/java"
grep -R -q "class PlatformDbtFreshnessClientTest" "$ROOT_DIR/dts-copilot-ai/src/test/java"
grep -R -q "accuracyEvidence" "$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot/CopilotChatContract.java"
grep -R -q "PlatformDbtFreshnessClient" "$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/platform"
grep -R -q "Nl2SqlAccuracyGoldenSetScorecardService" "$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot"
grep -R -q "Nl2SqlAccuracyGoldenSetReleaseEvidenceService" "$ROOT_DIR/dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/copilot"
grep -q "qa-finance-voucher-2026-main" "$ROOT_DIR/dts-copilot-ai/src/main/resources/governance/nl2sql-accuracy-golden-set.v1.json"
grep -R -q "可信度不足" "$ROOT_DIR/dts-copilot-webapp/src/components/copilot"
grep -R -q "不作为统计结论" "$ROOT_DIR/dts-copilot-webapp/src/components/copilot"

if grep -R --exclude='test_sprint34_accuracy_plan.sh' "TODO\\|TBD" "$SPRINT_DIR" >/dev/null; then
  echo "[sprint34-accuracy] placeholder text found" >&2
  exit 1
fi

(
  cd "$ROOT_DIR"
  mvn -q -pl dts-copilot-ai -Dtest='*Accuracy*,PlatformDbtFreshnessClientTest,CopilotChatRequestContextTest,AgentChatServiceTest,AgentExecutionServiceTest,Nl2SqlAccuracyGoldenSetReleaseEvidenceServiceTest' test
)

(
  cd "$ROOT_DIR/dts-copilot-webapp"
  pnpm exec vitest run src/api/modules/copilotStreamEvent.test.ts src/api/aiChatCompatibility.test.ts src/components/copilot/copilotStreamReducer.test.ts src/components/copilot/TracePanel.test.tsx src/components/copilot/MessageList.platformIndicator.test.tsx
)

echo "[sprint34-accuracy] plan ok"
