#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

cd "$REPO_ROOT"

scripts=(
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_worklog_consistency.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_completion_readiness.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_authority_registry_naming.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_application_mysql_authority_naming.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f1_authority_route_preflight_contract.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f1_authority_route_preflight.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_finance_authority_registry.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f1_detail_reconciliation.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f1_amount_column_alignment.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f2_summary_dual_reconciliation.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_proof.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f2_application_mysql_authority_runtime_case_ids_contract.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f2_finance_ingestion_mapping_preflight.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_voucher_tieout_mapping.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f2_voucher_subject_tieout.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_finance_invariant_registry.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f3_invariant_regression.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_static_caliber_guardrails.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f4_differential_grid.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_reconciliation_scorecard.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f4_scorecard_live_preflight_contract.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f4_scorecard_live_preflight.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f4_weak_path_reconciliation_candidates.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_answer_audit_trail.sh"
  "worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_signoff_baseline.sh"
)

for script in "${scripts[@]}"; do
  echo "[sprint33-local] running ${script}"
  bash "$script"
done

echo "[sprint33-local] contract gates ok (${#scripts[@]} scripts)"
