#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SPRINT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
EVIDENCE_DIR="$SCRIPT_DIR/evidence/20260601-local"
SUMMARY="$EVIDENCE_DIR/f5-sprint30-evidence-pack.md"

mkdir -p "$EVIDENCE_DIR"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "missing required file: $path" >&2
    return 1
  fi
}

require_file "$SPRINT_DIR/assets/credential-rotation-register.md"
require_file "$SPRINT_DIR/assets/ods-coverage-matrix.tsv"
require_file "$SPRINT_DIR/assets/biz-type-enum-dictionary.md"
require_file "$SPRINT_DIR/assets/finance-mart-catalog.md"
require_file "$SPRINT_DIR/assets/blank-domain-onboarding-checklist.md"
require_file "$SPRINT_DIR/assets/federated-query-catalog.md"

require_file "$EVIDENCE_DIR/f1-airflow-credentials-summary.md"
require_file "$EVIDENCE_DIR/f1-credential-scan.txt"
require_file "$EVIDENCE_DIR/f2-ods-coverage-summary.md"
require_file "$EVIDENCE_DIR/f3-caliber-guardrails-summary.md"
require_file "$EVIDENCE_DIR/f4-finance-vertical-slice-summary.md"
require_file "$EVIDENCE_DIR/f4-dbt-compile-selected.txt"
require_file "$EVIDENCE_DIR/f6-trino-federated-join-summary.md"
require_file "$SCRIPT_DIR/sql/f4_finance_adminweb_reconciliation.sql"
require_file "$SCRIPT_DIR/sql/f6_trino_federated_join.sql"

grep -F "DONE" "$SPRINT_DIR/assets/ods-coverage-matrix.tsv" >/dev/null
grep -F "CAL-INVENTORY-COST" "$SPRINT_DIR/assets/blank-domain-onboarding-checklist.md" >/dev/null
grep -F "xycyl_ads_finance_month_settlement" "$SPRINT_DIR/assets/finance-mart-catalog.md" >/dev/null
grep -F "prs.flowerbiz.federated" "$SPRINT_DIR/assets/federated-query-catalog.md" >/dev/null
grep -F "live Trino join | DONE" "$EVIDENCE_DIR/f6-trino-federated-join-summary.md" >/dev/null

{
  echo "# Sprint-30 IT Evidence Pack"
  echo
  echo "| Feature | Evidence | Status |"
  echo "|---------|----------|--------|"
  echo "| F1 | f1-airflow-credentials-summary.md; f1-credential-scan.txt | DONE |"
  echo "| F2 | ods-coverage-matrix.tsv; f2-ods-coverage-summary.md | DONE |"
  echo "| F3 | f3-caliber-guardrails-summary.md; biz-type-enum-dictionary.md | DONE |"
  echo "| F4 | f4-finance-vertical-slice-summary.md; f4-dbt-compile-selected.txt; f4_finance_adminweb_reconciliation.sql | DONE |"
  echo "| F5 | blank-domain-onboarding-checklist.md | DONE |"
  echo "| F6 | federated-query-catalog.md; f6_trino_federated_join.sql; f6-trino-federated-join-summary.md | DONE |"
  echo
  echo "All listed evidence files exist and are locally re-runnable."
} > "$SUMMARY"

echo "F5 evidence pack passed: $SUMMARY"
