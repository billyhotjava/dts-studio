# Data Connector Skills

Status: PLACEHOLDER — to be implemented in dts-data-connector

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| connector/discover-metadata | Rule | Auto-discover tables/columns from a data source | P0 |
| connector/test-connection | Rule | Test connectivity to a data source | P0 |
| connector/sync-cdc | Rule | Start CDC sync for a data source via Kafka | P1 |
| connector/suggest-mapping | LLM | Suggest source→Ontology field mapping | P1 |

## Supported Source Types

- JDBC: PostgreSQL, MySQL, Oracle, SQL Server, ClickHouse
- File: CSV, Excel, JSON, Parquet (via MinIO upload)
- API: REST endpoint with JSON response
- Industrial: OPC-UA (MES/SCADA), MQTT (IoT sensors)
- Big Data: Hive, Spark SQL (via JDBC)
