#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

cd "$REPO_ROOT"
mvn -q -pl dts-copilot-ai -Dtest=FinanceSignoffBaselineServiceTest test
cmp -s \
  dts-copilot-ai/src/main/resources/governance/finance-signoff-baseline.v1.json \
  worklog/v1.0.0/sprint-33-202607/assets/finance-signoff-baseline.v1.json
test -s worklog/v1.0.0/sprint-33-202607/assets/finance-signoff-baseline.md
! grep -n "TODO" worklog/v1.0.0/sprint-33-202607/assets/finance-signoff-baseline.md
