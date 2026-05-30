#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
CHECKLIST="$ROOT_DIR/worklog/v1.0.0/sprint-26-202605/assets/ontology-domain-onboarding-checklist.md"

require_text() {
  local pattern="$1"
  if ! grep -Fq "$pattern" "$CHECKLIST"; then
    echo "missing checklist text: $pattern" >&2
    exit 1
  fi
}

if [[ ! -f "$CHECKLIST" ]]; then
  echo "missing checklist file: $CHECKLIST" >&2
  exit 1
fi

require_text "links"
require_text "metrics"
require_text "signals"
require_text "actions"
require_text "OntologyService"
require_text "AssetBackedPlannerPolicy"
require_text "PACK_FILES"
require_text "normalizeSemanticDomain"
require_text "adminweb 对账"
require_text "adminapi 草稿端点"
require_text "saveDraft*"
require_text "Golden Questions"
require_text "项目域纸面演练"
require_text "xycyl_dim_project"
require_text "xycyl_dws_project_green_monthly"
require_text "p_project_green"
require_text "copilot.action.adminapi.base-url"
require_text "业务 Authorization"

echo "[static] ontology onboarding checklist covers pack/runtime/reconcile/action/project rehearsal"
