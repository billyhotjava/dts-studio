# Task 3.2: dts-query-service — SQL Query Execution

**Sprint:** 3 — Data Layer
**Points:** 6
**Status:** TODO

## Responsibility
- SQL query execution with tenant isolation
- Data source routing: PG (transactional) vs ClickHouse (analytical/time-series)
- SQL validation (injection prevention, scope checking)
- Result formatting (unified response)
- Calls dts-data-security for permission checks before returning data

## Key Design

```java
public class DataSourceRouter {
    public DataSource route(String sql, String namespace) {
        if (isTimeSeriesQuery(sql) || isAggregationQuery(sql)) {
            return clickHouseDataSource;
        }
        return postgresDataSource;
    }
}
```

## Steps

1. Create project skeleton
2. Write DataSourceRouter tests → implement
3. Write QueryValidator tests (SQL injection, scope) → implement
4. Write ResultFormatter tests → implement
5. Implement QueryGrpcService
6. Integration tests
7. Commit

```bash
git commit -m "feat(s3): implement dts-query-service with PG/CH routing"
```
