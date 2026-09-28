#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
GOLDEN_FILE="$ROOT_DIR/worklog/v1.0.0/sprint-26-202605/it/sql/flowerbiz_traversal_golden_questions.tsv"

row_count="$(grep -cvE '^(#|$)' "$GOLDEN_FILE")"
if [[ "$row_count" -lt 4 ]]; then
  echo "expected at least 4 traversal golden questions, got $row_count" >&2
  exit 1
fi

awk -F'\t' '
  !/^#/ && NF != 7 { printf("invalid golden row: %s\n", $0); exit 1 }
  !/^#/ && $4 !~ />/ && $1 != "G04" { printf("expected multi-link path for %s\n", $1); exit 1 }
  !/^#/ && $5 !~ /public\.xycyl_ads_flowerbiz_lease_detail/ { printf("missing lease detail ref for %s\n", $1); exit 1 }
' "$GOLDEN_FILE"

echo "[static] flowerbiz traversal golden questions are present"

if [[ "${RUN_TEST:-0}" == "1" ]]; then
  (cd "$ROOT_DIR" && mvn -pl dts-copilot-ai -Dtest=FlowerbizTraversalGoldenQuestionTest test)
  echo "[junit] traversal golden question regression passed"
fi
