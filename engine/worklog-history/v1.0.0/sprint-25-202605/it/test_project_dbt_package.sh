#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PKG_DIR="$ROOT_DIR/worklog/v1.0.0/sprint-25-202605/assets/xycyl-project-dbt-model"
ZIP_FILE="$ROOT_DIR/worklog/v1.0.0/sprint-25-202605/assets/xycyl-project-dbt-model.zip"

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

reject_path() {
  local path="$1"
  if [[ -e "$path" ]]; then
    echo "forbidden package path exists: $path" >&2
    exit 1
  fi
}

require_file "$PKG_DIR/dbt_project.yml"
require_file "$PKG_DIR/models/xycyl_project_sources.yml"
require_file "$PKG_DIR/models/xycyl_project_schema.yml"
require_file "$PKG_DIR/models/stg/xycyl_stg_project_project.sql"
require_file "$PKG_DIR/models/stg/xycyl_stg_project_customer.sql"
require_file "$PKG_DIR/models/stg/xycyl_stg_project_green.sql"
require_file "$PKG_DIR/models/dwd/xycyl_dim_project.sql"
require_file "$PKG_DIR/models/dwd/xycyl_dwd_project_green_snapshot.sql"
require_file "$PKG_DIR/models/dws/xycyl_dws_project_green_monthly.sql"
require_file "$PKG_DIR/models/ads/xycyl_ads_project_overview.sql"
require_file "$PKG_DIR/models/ads/xycyl_ads_contract_expiry_alert.sql"
require_file "$PKG_DIR/ods_ddl/project_ods_create_tables.sql"
require_file "$ZIP_FILE"

require_text "$PKG_DIR/dbt_project.yml" 'name: "xycyl_project"'
require_text "$PKG_DIR/dbt_project.yml" 'profile: "dts"'
require_text "$PKG_DIR/models/xycyl_project_sources.yml" 'schema: "public"'
require_text "$PKG_DIR/models/xycyl_project_sources.yml" 'identifier: "ods_ptr_mysql_p_project_green"'
require_text "$PKG_DIR/models/xycyl_project_sources.yml" 'name: "xycyl_project_ods"'
require_text "$PKG_DIR/models/dwd/xycyl_dim_project_status.sql" 'PRJ-ACTIVE'
require_text "$PKG_DIR/models/dwd/xycyl_dim_project_green_status.sql" 'PGS-PLACED'
require_text "$PKG_DIR/models/dwd/xycyl_dwd_project_green_snapshot.sql" 'is_orphan_project'
require_text "$PKG_DIR/models/dws/xycyl_dws_project_green_monthly.sql" 'rent_amount_raw_sum'
require_text "$PKG_DIR/README.md" '不使用独立 `xycyl_ods` schema'

reject_path "$PKG_DIR/models/stg/xycyl_stg_project.sql"
reject_path "$PKG_DIR/models/stg/xycyl_stg_customer.sql"
reject_path "$PKG_DIR/target"
reject_path "$PKG_DIR/logs"

unzip -t "$ZIP_FILE" >/dev/null
ZIP_LISTING="$(unzip -Z1 "$ZIP_FILE")"
grep -Fq "dbt_project.yml" <<< "$ZIP_LISTING"
grep -Fq "models/ads/xycyl_ads_project_overview.sql" <<< "$ZIP_LISTING"
grep -Fq "ods_ddl/project_ods_create_tables.sql" <<< "$ZIP_LISTING"
if grep -Eq '(^|/)(target|logs)(/|$)' <<< "$ZIP_LISTING"; then
  echo "forbidden dbt runtime path exists in zip" >&2
  exit 1
fi

if [[ "${RUN_DBT_PARSE:-0}" == "1" ]]; then
  : "${DBT_PROFILES_DIR:=$ROOT_DIR/worklog/v1.0.0/sprint-25-202605/it/profiles}"
  : "${DBT_TARGET_PATH:=/tmp/dts-copilot-sprint25-project-dbt-target}"
  dbt parse --project-dir "$PKG_DIR" --profiles-dir "$DBT_PROFILES_DIR" --target-path "$DBT_TARGET_PATH"
fi
