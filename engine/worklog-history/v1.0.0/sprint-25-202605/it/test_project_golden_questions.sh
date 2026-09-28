#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SPRINT_DIR="$ROOT_DIR/worklog/v1.0.0/sprint-25-202605"
GOLDEN_FILE="$SPRINT_DIR/it/sql/project_golden_questions.tsv"
EVIDENCE_FILE="$SPRINT_DIR/it/evidence/20260530-local/project-golden-questions.md"

fail() {
  echo "$1" >&2
  exit 1
}

require_file() {
  local path="$1"
  [[ -f "$path" ]] || fail "missing required file: $path"
}

require_text() {
  local path="$1"
  local pattern="$2"
  grep -Fq "$pattern" "$path" || fail "missing text in $path: $pattern"
}

require_file "$GOLDEN_FILE"

row_count="$(awk -F'\t' '!/^#/ && NF { count++ } END { print count + 0 }' "$GOLDEN_FILE")"
[[ "$row_count" -ge 15 ]] || fail "expected at least 15 project golden questions, got $row_count"

fast_path_count="$(awk -F'\t' '!/^#/ && NF && $4 == "MART_FAST_PATH" { count++ } END { print count + 0 }' "$GOLDEN_FILE")"
[[ "$fast_path_count" -ge 8 ]] || fail "expected at least 8 mart fast path questions, got $fast_path_count"

unique_fast_templates="$(awk -F'\t' '!/^#/ && NF && $4 == "MART_FAST_PATH" { seen[$6] = 1 } END { for (k in seen) count++; print count + 0 }' "$GOLDEN_FILE")"
[[ "$unique_fast_templates" -ge 8 ]] || fail "expected at least 8 unique fast path templates, got $unique_fast_templates"

awk -F'\t' '
  !/^#/ && NF && NF != 8 {
    printf("invalid golden row, expected 8 columns: %s\n", $0);
    exit 1
  }
  !/^#/ && NF && $3 != "project" {
    printf("invalid domain for %s: %s\n", $1, $3);
    exit 1
  }
  !/^#/ && NF && $4 !~ /^(MART_FAST_PATH|OBJECT_PROFILE|CLARIFICATION_GUARD)$/ {
    printf("invalid route for %s: %s\n", $1, $4);
    exit 1
  }
  !/^#/ && NF && $5 ~ /(v_project_|mart_project_fulfillment_daily|xycyl_ods)/ {
    printf("legacy target forbidden for %s: %s\n", $1, $5);
    exit 1
  }
  !/^#/ && NF && $4 == "MART_FAST_PATH" && $5 !~ /^public\.xycyl_/ {
    printf("mart fast path must target dbt public.xycyl_* for %s: %s\n", $1, $5);
    exit 1
  }
  !/^#/ && NF && $4 == "MART_FAST_PATH" && $6 !~ /^TPL-[0-9]+$/ {
    printf("mart fast path must carry template code for %s: %s\n", $1, $6);
    exit 1
  }
' "$GOLDEN_FILE"

for template_code in TPL-44 TPL-45 TPL-46 TPL-47 TPL-48 TPL-49 TPL-50 TPL-51; do
  require_text "$GOLDEN_FILE" "$template_code"
done

for target in \
  "public.xycyl_ads_project_overview" \
  "public.xycyl_ads_contract_expiry_alert" \
  "public.xycyl_ads_project_status_dist" \
  "public.xycyl_ads_project_green_change_monthly" \
  "public.xycyl_dws_project_green_monthly" \
  "public.xycyl_dwd_position_adjustment" \
  "public.xycyl_dwd_project_green_snapshot" \
  "business-object:prs.project.green_snapshot"; do
  require_text "$GOLDEN_FILE" "$target"
done

require_file "$EVIDENCE_FILE"
require_text "$EVIDENCE_FILE" "Golden Questions: 15/15"
require_text "$EVIDENCE_FILE" "Mart fast path: 12/15"
require_text "$EVIDENCE_FILE" "Unique fast templates: 8/8"
require_text "$EVIDENCE_FILE" "RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_golden_questions.sh"

echo "[static] project golden questions are present"

if [[ "${RUN_LIVE:-0}" == "1" ]]; then
  : "${DTS_PG_CONTAINER:=v223-dts-pg-1}"
  : "${DTS_PG_USER:=biadmin}"
  : "${DTS_PG_DB:=biadmin}"

  tmp_sql="$(mktemp)"
  trap 'rm -f "$tmp_sql"' EXIT

  {
    echo "\\set ON_ERROR_STOP on"
    echo "WITH required(target) AS (VALUES"
    awk -F'\t' '
      !/^#/ && NF && $5 ~ /^public\./ {
        if (seen[$5]++) next;
        printf("  (%c%s%c),\n", 39, $5, 39)
      }
    ' "$GOLDEN_FILE" | sed '$ s/,$//'
    echo ")"
    echo "SELECT target, to_regclass(target) AS relation"
    echo "FROM required"
    echo "WHERE to_regclass(target) IS NULL;"
  } > "$tmp_sql"

  missing="$(docker exec -i "$DTS_PG_CONTAINER" psql -U "$DTS_PG_USER" -d "$DTS_PG_DB" -At -f - < "$tmp_sql")"
  [[ -z "$missing" ]] || fail "missing live mart targets: $missing"

  docker exec -i "$DTS_PG_CONTAINER" psql -U "$DTS_PG_USER" -d "$DTS_PG_DB" -At <<'SQL' | awk -F'|' '$2 <= 0 { printf("target has no rows: %s\n", $1); exit 1 }'
SELECT 'public.xycyl_ads_project_overview', COUNT(*) FROM public.xycyl_ads_project_overview;
SELECT 'public.xycyl_ads_project_status_dist', COUNT(*) FROM public.xycyl_ads_project_status_dist;
SELECT 'public.xycyl_ads_project_green_change_monthly', COUNT(*) FROM public.xycyl_ads_project_green_change_monthly;
SELECT 'public.xycyl_dws_project_green_monthly', COUNT(*) FROM public.xycyl_dws_project_green_monthly;
SELECT 'public.xycyl_dwd_position_adjustment', COUNT(*) FROM public.xycyl_dwd_position_adjustment;
SELECT 'public.xycyl_dwd_project_green_snapshot', COUNT(*) FROM public.xycyl_dwd_project_green_snapshot;
SQL

  echo "[live] project golden question mart targets exist and populated"
fi
