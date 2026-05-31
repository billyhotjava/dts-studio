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
require_file "$SPRINT_DIR/it/sql/project_ingestion_task_upsert.sql"
require_file "$SPRINT_DIR/it/sql/project_source_profile.sql"
require_file "$SPRINT_DIR/it/sql/project_golden_questions.tsv"
require_file "$SPRINT_DIR/it/sql/project_adminweb_summary_reconcile.sql"
require_file "$SPRINT_DIR/it/test_project_dbt_package.sh"
require_file "$SPRINT_DIR/it/test_project_ingestion_runtime.sh"
require_file "$SPRINT_DIR/it/test_project_golden_questions.sh"
require_file "$SPRINT_DIR/it/test_project_adminweb_reconcile.sh"
require_file "$SPRINT_DIR/it/test_project_source_profile_sql.sh"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model.zip"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model/dbt_project.yml"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/xycyl_project_sources.yml"
require_file "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/xycyl_project_schema.yml"
require_file "$ROOT_DIR/dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_021__project_dbt_query_templates.xml"
require_file "$ROOT_DIR/dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_022__project_adminweb_query_templates.xml"
require_file "$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/project-fulfillment.json"
require_file "$SPRINT_DIR/it/evidence/20260529-local/project-ods-create-tables.md"
require_file "$SPRINT_DIR/it/evidence/20260529-local/project-dbt-package.md"
require_file "$SPRINT_DIR/it/evidence/20260529-local/project-source-profile.md"
require_file "$SPRINT_DIR/it/evidence/20260530-local/project-ingestion-runtime.md"
require_file "$SPRINT_DIR/it/evidence/20260530-local/project-profile-after-ingestion.md"
require_file "$SPRINT_DIR/it/evidence/20260530-local/project-dbt-build-after-ingestion.md"
require_file "$SPRINT_DIR/it/evidence/20260530-local/project-nl2sql-dbt-routing.md"
require_file "$SPRINT_DIR/it/evidence/20260530-local/project-golden-questions.md"
require_file "$SPRINT_DIR/it/evidence/20260530-local/project-adminweb-reconcile.md"

require_text "$SPRINT_DIR/README.md" "Sprint-25: 项目管理域 + 共享维度数据面"
require_text "$SPRINT_DIR/README.md" "P0 口径决策全部 RESOLVED 才能进 P1"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "public.ods_ptr_mysql_p_project"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "public.ods_ptr_mysql_p_project_green"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "public.ods_ptr_mysql_p_position_adjustment"
require_text "$SPRINT_DIR/assets/project-source-catalog.md" "POPULATED (2026-05-30 task 46)"
require_text "$SPRINT_DIR/assets/project-caliber-decisions.md" "决策 1：p_project_green 快照粒度"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "xycyl_dim_project"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "xycyl_ads_project_overview"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "assets/xycyl-project-dbt-model.zip"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "PASS=76 WARN=1 ERROR=0"
require_text "$SPRINT_DIR/assets/project-dbt-model-catalog.md" "adminweb ProjectSummary"
require_text "$SPRINT_DIR/assets/xycyl-project-dbt-model/dbt_project.yml" 'name: "xycyl_project"'
require_text "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/xycyl_project_sources.yml" 'schema: "public"'
require_text "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/dwd/xycyl_dim_project_status.sql" "PRJ-ACTIVE"
require_text "$SPRINT_DIR/assets/xycyl-project-dbt-model/models/xycyl_project_schema.yml" "rent_amount_adminweb_sum"
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
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-ingestion-runtime.md" "sprint25_project_datasurface"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-ingestion-runtime.md" "manual__2026-05-30T15:24:11.351264+00:00"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-profile-after-ingestion.md" "parent_id=-1"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-dbt-build-after-ingestion.md" "PASS=76 WARN=1 ERROR=0"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-nl2sql-dbt-routing.md" "TPL-44"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-nl2sql-dbt-routing.md" "public.xycyl_ads_project_overview"
require_text "$SPRINT_DIR/it/sql/project_golden_questions.tsv" "PG15"
require_text "$SPRINT_DIR/it/sql/project_golden_questions.tsv" "CLARIFICATION_GUARD"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-golden-questions.md" "Golden Questions: 15/15"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-golden-questions.md" "Unique fast templates: 8/8"
require_text "$SPRINT_DIR/it/sql/project_adminweb_summary_reconcile.sql" "ProjectSummaryMapper.xml"
require_text "$SPRINT_DIR/it/sql/project_adminweb_summary_reconcile.sql" "diff_pct > 0.005"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-adminweb-reconcile.md" "cost_amount_adminweb_sum"
require_text "$SPRINT_DIR/it/evidence/20260530-local/project-adminweb-reconcile.md" "7/7 PASS"
require_text "$ROOT_DIR/dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_021__project_dbt_query_templates.xml" "TPL-44"
require_text "$ROOT_DIR/dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_022__project_adminweb_query_templates.xml" "rent_amount_adminweb_sum"
require_text "$ROOT_DIR/dts-copilot-ai/src/main/resources/semantic-packs/project-fulfillment.json" "real_good_number_adminweb_sum"

bash "$SPRINT_DIR/it/test_project_source_profile_sql.sh"
bash "$SPRINT_DIR/it/test_project_dbt_package.sh"
bash "$SPRINT_DIR/it/test_project_ingestion_runtime.sh"
bash "$SPRINT_DIR/it/test_project_golden_questions.sh"
bash "$SPRINT_DIR/it/test_project_adminweb_reconcile.sh"
