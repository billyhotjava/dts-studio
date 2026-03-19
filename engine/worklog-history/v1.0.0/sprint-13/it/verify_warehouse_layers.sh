#!/usr/bin/env bash
set -euo pipefail

PG_HOST="${PG_HOST:-127.0.0.1}"
PG_PORT="${PG_PORT:-15432}"
PG_DB="${PG_DB:-copilot}"
PG_USER="${PG_USER:-copilot}"
PG_PASSWORD="${PG_PASSWORD:-copilot_dev}"
PG_SCHEMA="${PG_SCHEMA:-copilot_analytics}"
POSTGRES_JDBC_JAR="${POSTGRES_JDBC_JAR:-$HOME/.m2/repository/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar}"

if [[ ! -f "${POSTGRES_JDBC_JAR}" ]]; then
  echo "PostgreSQL JDBC jar not found: ${POSTGRES_JDBC_JAR}" >&2
  exit 1
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "javac is required for verify_warehouse_layers.sh" >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

java_file="${tmp_dir}/WarehouseLayerVerifier.java"

python3 - "${java_file}" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
path.write_text(
    """import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class WarehouseLayerVerifier {

    private static final Set<String> EXPECTED_TABLES = Set.of(
        "mart_project_fulfillment_daily",
        "fact_field_operation_event",
        "elt_sync_watermark"
    );

    private static final Set<String> EXPECTED_VIEWS = Set.of(
        "v_project_overview",
        "v_flower_biz_detail",
        "v_project_green_current",
        "v_monthly_settlement",
        "v_task_progress",
        "v_curing_coverage",
        "v_pendulum_progress"
    );

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("expected args: url user password schema");
        }

        String url = args[0];
        String user = args[1];
        String password = args[2];
        String schema = args[3];

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            Set<String> actualTables = queryTables(connection, schema);
            for (String table : EXPECTED_TABLES) {
                if (!actualTables.contains(table)) {
                    throw new IllegalStateException("missing expected table: " + table);
                }
            }

            ensureAbsentByPrefix(actualTables, "dwd_");
            ensureAbsentByPrefix(actualTables, "dws_");
            ensureAbsentByPrefix(actualTables, "ads_");

            ensureColumns(connection, schema, "mart_project_fulfillment_daily",
                Set.of("snapshot_date", "project_id", "sync_batch_id", "synced_at"));
            ensureColumns(connection, schema, "fact_field_operation_event",
                Set.of("event_date", "event_month", "biz_type_name", "source_updated_at", "sync_batch_id"));
            ensureColumns(connection, schema, "elt_sync_watermark",
                Set.of("target_table", "last_watermark", "last_sync_time", "last_sync_rows", "last_sync_duration_ms", "sync_status"));

            Set<String> registryViews = queryBusinessViews(connection);
            for (String view : EXPECTED_VIEWS) {
                if (!registryViews.contains(view)) {
                    throw new IllegalStateException("missing business view registry row: " + view);
                }
            }

            System.out.println("[IT-WH-01] physical ELT tables: " + actualTables);
            System.out.println("[IT-WH-02] business view registry: " + registryViews);
            System.out.println("[IT-WH-03] warehouse layering decision: mart/fact + view registry only");
            System.out.println("PASS verify_warehouse_layers");
        }
    }

    private static Set<String> queryTables(Connection connection, String schema) throws Exception {
        Set<String> tables = new LinkedHashSet<>();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? ORDER BY table_name";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private static void ensureAbsentByPrefix(Set<String> tables, String prefix) {
        for (String table : tables) {
            if (table.startsWith(prefix)) {
                throw new IllegalStateException("unexpected warehouse layer table found: " + table);
            }
        }
    }

    private static void ensureColumns(Connection connection, String schema, String table, Set<String> expectedColumns) throws Exception {
        Set<String> columns = new LinkedHashSet<>();
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString(1));
                }
            }
        }
        for (String expected : expectedColumns) {
            if (!columns.contains(expected)) {
                throw new IllegalStateException("missing expected column " + expected + " in " + table);
            }
        }
    }

    private static Set<String> queryBusinessViews(Connection connection) throws Exception {
        Set<String> views = new LinkedHashSet<>();
        String sql = "SELECT view_name FROM business_view_registry ORDER BY view_name";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                views.add(rs.getString(1));
            }
        }
        return views;
    }
}
""",
    encoding="utf-8",
)
PY

javac -cp "${POSTGRES_JDBC_JAR}" "${java_file}"

java -cp "${tmp_dir}:${POSTGRES_JDBC_JAR}" WarehouseLayerVerifier \
  "jdbc:postgresql://${PG_HOST}:${PG_PORT}/${PG_DB}?currentSchema=${PG_SCHEMA}" \
  "${PG_USER}" \
  "${PG_PASSWORD}" \
  "${PG_SCHEMA}"
