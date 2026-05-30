#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SPRINT_DIR="$ROOT_DIR/worklog/v1.0.0/sprint-25-202605"
ROLLOUT_PLAN="$ROOT_DIR/../docs/plans/2026-05-29-xycyl-three-domain-datasurface-rollout-plan.md"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "missing required file: $path" >&2
    exit 1
  fi
}

require_text() {
  local path="$1"
  local pattern="$2"
  if ! grep -Fq "$pattern" "$path"; then
    echo "missing text in $path: $pattern" >&2
    exit 1
  fi
}

reject_text() {
  local path="$1"
  local pattern="$2"
  if grep -Fq "$pattern" "$path"; then
    echo "forbidden text in $path: $pattern" >&2
    exit 1
  fi
}

require_file "$ROLLOUT_PLAN"
require_text "$ROLLOUT_PLAN" "public.ods_ptr_mysql_"
reject_text "$ROLLOUT_PLAN" "xycyl_ods"

require_file "$SPRINT_DIR/README.md"
require_file "$SPRINT_DIR/assets/project-source-catalog.md"
require_file "$SPRINT_DIR/assets/project-caliber-decisions.md"
require_file "$SPRINT_DIR/assets/project-dbt-model-catalog.md"
require_file "$SPRINT_DIR/features/F0-项目域P0数据画像与口径决策/README.md"
require_file "$SPRINT_DIR/features/F1-共享维度与项目域dbt建模/README.md"
require_file "$SPRINT_DIR/features/F2-项目域NL2SQL接入/README.md"
require_file "$SPRINT_DIR/it/README.md"
require_file "$SPRINT_DIR/it/sql/project_ods_create_tables.sql"
require_file "$SPRINT_DIR/it/sql/project_source_profile.sql"
require_file "$SPRINT_DIR/it/test_project_dbt_package.sh"
require_file "$SPRINT_DIR/it/test_project_source_profile_sql.sh"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model.zip"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model/dbt_project.yml"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/xycyl_project_sources.yml"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/xycyl_project_schema.yml"
require_file "$SPRINT_DIR/it/evidence/20260529-local/project-ods-create-tables.md"
require_file "$SPRINT_DIR/it/evidence/20260529-local/project-dbt-package.md"
require_file "$SPRINT_DIR/it/evidence/20260529-local/project-source-profile.md"

require_text "$SPRINT_DIR/README.md" "Sprint-25: 项目管理域 + 共享维度数据面"
require_text "$SPRINT_DIR/README.md" "P0 口径决策全部 RESOLVED 才能进 P1"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "public.ods_ptr_mysql_p_project"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "public.ods_ptr_mysql_p_project_green"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "public.ods_ptr_mysql_p_position_adjustment"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "FOUND (2026-05-29 local biadmin)"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "CREATED EMPTY (2026-05-29 local biadmin)"
require_text "$SPRINT_DIR/assets/project-caliber-decisions.md" "决策 1：p_project_green 快照粒度"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "xycyl_dim_project"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "xycyl_ads_project_overview"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "assets/xycyl-project-dbt-model.zip"
require_text "$SPRINT_DIR/assets/xycyl-project-dbt-model/dbt_project.yml" 'name: "xycyl_project"'
require_text "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/xycyl_project_sources.yml" 'schema: "public"'
require_text "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/dwd/xycyl_dim_project_status.sql" "PRJ-ACTIVE"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_p_contract"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_p_position"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_p_floor_layer"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_p_floor_number"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_b_goods"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_b_goods_price"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_p_project_green"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_p_position_adjustment"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_p_position_adjustment_item"
require_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "_dts_import_time"
reject_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "xycyl_ods"
reject_text "$SPRINT_DIR/it/sql/project_ods_create_tables.sql" "DROP TABLE"
require_text "$SPRINT_DIR/it/sql/project_source_profile.sql" "to_regclass('public.ods_ptr_mysql_p_project_green')"
require_text "$SPRINT_DIR/it/sql/project_source_profile.sql" "\\gexec"
reject_text "$SPRINT_DIR/it/sql/project_source_profile.sql" "xycyl_ods"
require_text "$SPRINT_DIR/it/evidence/20260529-local/project-ods-create-tables.md" "applied: 9 ODS tables"
require_text "$SPRINT_DIR/it/evidence/20260529-local/project-dbt-package.md" "dbt run: PASS=27"
require_text "$SPRINT_DIR/it/evidence/20260529-local/project-dbt-package.md" "dbt test: PASS=50"
require_text "$SPRINT_DIR/it/evidence/20260529-local/project-source-profile.md" "ods_ptr_mysql_p_project_green | FOUND"

bash "$SPRINT_DIR/it/test_project_source_profile_sql.sh"
bash "$SPRINT_DIR/it/test_project_dbt_package.sh"
