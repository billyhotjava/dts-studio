#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../../../.." && pwd)"
COPILOT_DIR="$ROOT_DIR/dts-copilot"
DTS_STACK_DIR="${DTS_STACK_DIR:-$ROOT_DIR/dts-stack}"
DAG_DIR="$DTS_STACK_DIR/services/dts-airflow/dags"
OUT_DIR="${SPRINT30_EVIDENCE_DIR:-$COPILOT_DIR/worklog/v1.0.0/sprint-30-202606/it/evidence/$(date +%Y%m%d)-local}"
ASSET_DIR="$COPILOT_DIR/worklog/v1.0.0/sprint-30-202606/assets"
mkdir -p "$OUT_DIR" "$ASSET_DIR"

REQUIRED_TABLES=(
  t_change_info
  t_warehousing_info
  t_warehousing_item
  t_ex_warehouse_info
  t_ex_warehouse_item
  a_month_accounting
  a_green_accounting
  a_flower_biz_accounting
  a_sale_account
  a_sale_account_rent_item
  a_invoice_info
  a_invoice_item
  a_invoice_record
  a_collection_record
  a_collection_item
)

COVERAGE_TSV="$ASSET_DIR/ods-coverage-matrix.tsv"
MISSING_TSV="$OUT_DIR/f2-ods-missing.tsv"
PASSWORD_TSV="$OUT_DIR/f2-ods-password-placeholders.tsv"

printf 'source_table\tods_table\tjson_count\tstatus\n' > "$COVERAGE_TSV"
: > "$MISSING_TSV"
: > "$PASSWORD_TSV"

for table in "${REQUIRED_TABLES[@]}"; do
  ods_table="ods_ptr_mysql_${table}"
  count="$(find "$DAG_DIR" -maxdepth 1 -type f -name "ptr_mysql_flow-*_${ods_table}.json" | wc -l | tr -d ' ')"
  status="DONE"
  if [[ "$count" != "5" ]]; then
    status="MISSING"
    printf '%s\t%s\t%s\n' "$table" "$ods_table" "$count" >> "$MISSING_TSV"
  fi
  printf '%s\t%s\t%s\t%s\n' "$table" "$ods_table" "$count" "$status" >> "$COVERAGE_TSV"

  while IFS= read -r job_file; do
    jq -r '
      .job.content[]
      | [
          input_filename,
          .reader.parameter.connection[0].table[0],
          .writer.parameter.connection[0].table[0],
          .reader.parameter.password,
          .writer.parameter.password
        ]
      | @tsv
    ' "$job_file" >> "$PASSWORD_TSV"
  done < <(find "$DAG_DIR" -maxdepth 1 -type f -name "ptr_mysql_flow-*_${ods_table}.json" | sort)
done

if [[ -s "$MISSING_TSV" ]]; then
  echo "FAIL: ODS coverage gaps remain; see $MISSING_TSV" >&2
  exit 1
fi

if ! awk -F '\t' '
  $4 != "${DTS_ADDAX_READER_PASSWORD}" || $5 != "${DTS_TARGET_DB_PASSWORD}" { bad=1 }
  END { exit bad }
' "$PASSWORD_TSV"; then
  echo "FAIL: ODS Addax JSON password placeholders are not normalized; see $PASSWORD_TSV" >&2
  exit 1
fi

cat > "$OUT_DIR/f2-ods-coverage-summary.md" <<EOF
# F2 ODS Coverage Verification

- Required source tables: ${#REQUIRED_TABLES[@]}
- Expected JSON files per source table: 5
- Coverage matrix: $COVERAGE_TSV
- Password placeholder report: $PASSWORD_TSV
EOF

echo "PASS: F2 ODS coverage checks wrote evidence to $OUT_DIR"
