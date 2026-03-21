#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
WEBAPP_DIR="${ROOT_DIR}/dts-copilot-webapp"

echo "[S19-IT-01] query asset center draft tests"
node --experimental-strip-types --test \
  "${WEBAPP_DIR}/tests/analysisDraftReuseModel.test.ts" \
  "${WEBAPP_DIR}/tests/analysisDraftSurfaceEntry.test.ts" \
  "${WEBAPP_DIR}/tests/queryAssetCenterModel.test.ts" \
  "${WEBAPP_DIR}/tests/queryDraftHandoff.test.ts" \
  "${WEBAPP_DIR}/tests/copilotAnalysisDraft.test.ts"

echo "[S19-IT-02] frontend typecheck"
pnpm --dir "${WEBAPP_DIR}" run typecheck

echo "[S19-IT-03] frontend build"
pnpm --dir "${WEBAPP_DIR}" run build

echo "PASS test_analysis_draft_workflow"
