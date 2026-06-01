#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../../../.." && pwd)"
DTS_STACK_DIR="${DTS_STACK_DIR:-$ROOT_DIR/dts-stack}"
if [[ ! -d "$DTS_STACK_DIR" ]]; then
  DTS_STACK_DIR="$ROOT_DIR/../dts-stack"
fi

COPILOT_DIR="$ROOT_DIR/dts-copilot"
DAG_DIR="$DTS_STACK_DIR/services/dts-airflow/dags"
SCAN_SCRIPT="$DTS_STACK_DIR/scripts/security/sprint30_credential_scan.sh"
OUT_DIR="${SPRINT30_EVIDENCE_DIR:-$COPILOT_DIR/worklog/v1.0.0/sprint-30-202606/it/evidence/$(date +%Y%m%d)-local}"
mkdir -p "$OUT_DIR"

TARGET_LIST="$OUT_DIR/f1-ptr-mysql-dag-json-files.txt"
EXPOSED_FILES="$OUT_DIR/f1-exposed-password-files.txt"
PLACEHOLDER_REPORT="$OUT_DIR/f1-addax-password-placeholders.tsv"
SCAN_REPORT="$OUT_DIR/f1-credential-scan.txt"

F1_TABLES=(
  p_customer
  p_project
  t_change_info
  t_flower_biz_info
  t_flower_biz_item
  t_flower_biz_item_detailed
  t_flower_biz_log
  t_flower_extra_cost
  t_flower_rent_time_log
  t_recovery_info
  t_recovery_info_item
)

: > "$TARGET_LIST"
for table in "${F1_TABLES[@]}"; do
  find "$DAG_DIR" -maxdepth 1 -type f -name "ptr_mysql_flow-*_ods_ptr_mysql_${table}.json" | sort >> "$TARGET_LIST"
done
target_count="$(wc -l < "$TARGET_LIST" | tr -d ' ')"
if [[ "$target_count" != "55" ]]; then
  echo "FAIL: expected 55 ptr_mysql_flow ODS Addax JSON files, got $target_count" >&2
  exit 1
fi

if rg -l 'Devops[0-9]+@' "$DAG_DIR" --glob 'ptr_mysql_flow-*_ods_*.json' > "$EXPOSED_FILES"; then
  echo "FAIL: exposed password literal still appears in target Airflow Addax JSON files; see $EXPOSED_FILES" >&2
  exit 1
fi

: > "$PLACEHOLDER_REPORT"
while IFS= read -r job_file; do
  jq -r '
    .job.content[]
    | [
        input_filename,
        .reader.parameter.password,
        .writer.parameter.password
      ]
    | @tsv
  ' "$job_file" >> "$PLACEHOLDER_REPORT"
done < "$TARGET_LIST"

if ! awk -F '\t' '
  $2 != "${DTS_ADDAX_READER_PASSWORD}" || $3 != "${DTS_TARGET_DB_PASSWORD}" { bad=1 }
  END { exit bad }
' "$PLACEHOLDER_REPORT"; then
  echo "FAIL: Addax JSON password placeholders are not normalized; see $PLACEHOLDER_REPORT" >&2
  exit 1
fi

if [[ ! -x "$DAG_DIR/addax-env-runner.sh" ]]; then
  echo "FAIL: Addax runtime credential renderer is missing or not executable: $DAG_DIR/addax-env-runner.sh" >&2
  exit 1
fi

if [[ ! -x "$SCAN_SCRIPT" ]]; then
  echo "FAIL: Sprint-30 credential scan script is missing or not executable: $SCAN_SCRIPT" >&2
  exit 1
fi

"$SCAN_SCRIPT" > "$SCAN_REPORT"

cat > "$OUT_DIR/f1-airflow-credentials-summary.md" <<EOF
# F1 Airflow Credentials Verification

- Target Addax JSON files: $target_count
- Exposed literal file list: $EXPOSED_FILES
- Placeholder report: $PLACEHOLDER_REPORT
- Credential scan report: $SCAN_REPORT
EOF

echo "PASS: F1 Airflow credential checks wrote evidence to $OUT_DIR"
