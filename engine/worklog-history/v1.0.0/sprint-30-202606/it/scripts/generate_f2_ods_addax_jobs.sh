#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../../../../.." && pwd)"
DTS_STACK_DIR="${DTS_STACK_DIR:-$ROOT_DIR/dts-stack}"
DAG_DIR="$DTS_STACK_DIR/services/dts-airflow/dags"
SRC_SQL="$ROOT_DIR/adminapi/docker/mysql/db/rs_cloud_flower.sql"
INVOICE_SQL="$ROOT_DIR/adminapi/docker/mysql/db/add_invoice_record_table.sql"

SOURCE_JDBC="$(jq -r '.job.content[0].reader.parameter.connection[0].jdbcUrl[0]' "$DAG_DIR/ptr_mysql_flow-448bde1e_ods_ptr_mysql_t_change_info.json")"
mapfile -t TENANTS < <(
  find "$DAG_DIR" -maxdepth 1 -type f -name "ptr_mysql_flow-*_ods_ptr_mysql_t_change_info.json" -printf "%f\n" \
    | sed -E "s/^ptr_mysql_flow-([^-_]+)_.*$/\1/" \
    | sort
)

TABLES=(
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

extract_columns() {
  local table="$1"
  awk -v table="$table" '
    BEGIN { in_table=0 }
    $0 ~ "CREATE TABLE( IF NOT EXISTS)? `" table "`" { in_table=1; next }
    in_table && /^\) ENGINE/ { in_table=0 }
    in_table && /^[[:space:]]*`/ {
      gsub(/^[[:space:]]*`/, "")
      sub(/`.*/, "")
      print
    }
  ' "$SRC_SQL" "$INVOICE_SQL"
}

for table in "${TABLES[@]}"; do
  mapfile -t columns < <(extract_columns "$table")
  if [[ "${#columns[@]}" -eq 0 ]]; then
    echo "ERROR: missing columns for $table" >&2
    exit 1
  fi
  columns_json="$(printf "%s\n" "${columns[@]}" | jq -R . | jq -s .)"
  target="ods_ptr_mysql_${table}"
  for tenant in "${TENANTS[@]}"; do
    out="$DAG_DIR/ptr_mysql_flow-${tenant}_${target}.json"
    jq -n \
      --arg sourceJdbc "$SOURCE_JDBC" \
      --arg table "$table" \
      --arg target "$target" \
      --argjson columns "$columns_json" \
      '
      {
        job: {
          setting: { speed: { channel: 1 } },
          content: [
            {
              reader: {
                parameter: {
                  connection: [ { jdbcUrl: [ $sourceJdbc ], table: [ $table ] } ],
                  username: "root",
                  password: "${DTS_ADDAX_READER_PASSWORD}",
                  column: ["*"],
                  readerType: "mysqlreader",
                  sourceSystem: "ptr_mysql",
                  driver: "com.mysql.cj.jdbc.Driver"
                },
                name: "mysqlreader"
              },
              writer: {
                parameter: {
                  column: $columns,
                  jdbcUrl: "jdbc:postgresql://dts-pg:5432/biadmin?sslmode=disable",
                  username: "biadmin",
                  password: "${DTS_TARGET_DB_PASSWORD}",
                  writeMode: "insert",
                  connection: [ { table: [ $target ], jdbcUrl: "jdbc:postgresql://dts-pg:5432/biadmin?sslmode=disable" } ],
                  writerType: "postgresqlwriter",
                  tablePrefix: "ods_ptr_mysql_",
                  driver: "org.postgresql.Driver",
                  postSql: [
                    "ALTER TABLE \"\($target)\" ADD COLUMN IF NOT EXISTS \"_dts_source_system\" VARCHAR(500)",
                    "ALTER TABLE \"\($target)\" ADD COLUMN IF NOT EXISTS \"_dts_source_table\" VARCHAR(500)",
                    "ALTER TABLE \"\($target)\" ADD COLUMN IF NOT EXISTS \"_dts_import_time\" TIMESTAMP",
                    "ALTER TABLE \"\($target)\" ADD COLUMN IF NOT EXISTS \"_dts_batch_id\" VARCHAR(500)",
                    "ALTER TABLE \"\($target)\" ADD COLUMN IF NOT EXISTS \"_dts_execution_id\" VARCHAR(500)",
                    "ALTER TABLE \"\($target)\" ADD COLUMN IF NOT EXISTS \"_dts_task_id\" VARCHAR(500)",
                    "UPDATE \"\($target)\" SET \"_dts_source_system\" = '\''ptr_mysql'\'', \"_dts_source_table\" = '\''\($table)'\'', \"_dts_import_time\" = timezone('\''Asia/Shanghai'\'', now()), \"_dts_batch_id\" = '\''unknown'\'', \"_dts_execution_id\" = '\''unknown'\'', \"_dts_task_id\" = '\''unknown'\'' WHERE TRUE"
                  ],
                  preSql: [ "TRUNCATE TABLE \($target)" ]
                },
                name: "postgresqlwriter"
              }
            }
          ]
        }
      }
      ' > "$out"
  done
done

printf "generated %d source tables for %d tenants\n" "${#TABLES[@]}" "${#TENANTS[@]}"
