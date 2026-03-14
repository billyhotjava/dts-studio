# NV-01: 默认数据源自动注册

**状态**: READY
**依赖**: BA-02

## 目标

应用启动时自动注册预配置的业务数据库，并触发元数据同步（表/字段扫描），实现开箱即用。

## 技术设计

### 配置格式（application.yml）

```yaml
dts:
  analytics:
    default-databases:
      - name: "园林业务库"
        engine: postgresql
        host: ${BIZ_DB_HOST:localhost}
        port: ${BIZ_DB_PORT:5432}
        db: ${BIZ_DB_NAME:garden}
        user: ${BIZ_DB_USER:readonly}
        password: ${BIZ_DB_PASSWORD:}
        auto-sync-metadata: true
```

### 新建 DefaultDatabaseInitService

- 实现 `ApplicationRunner`
- 遍历 `dts.analytics.default-databases` 配置列表
- 对每个配置项，按 `name` 查 `AnalyticsDatabaseRepository`
- 不存在则创建 `AnalyticsDatabase` 记录：
  - `engine` = 配置值
  - `detailsJson` = `{"host":"...", "port":..., "db":"...", "user":"...", "password":"..."}` （参考现有记录格式）
  - `name` = 配置值
  - `autoRunQueries` = true, `fullSync` = true
- 创建后，如果 `auto-sync-metadata=true`，调用现有的元数据同步逻辑（JDBC `DatabaseMetaData` → 写入 `analytics_table` + `analytics_field`）

### 配置类

在 `ApplicationProperties` 中增加 `default-databases` 内嵌配置记录类。

## 影响文件

- `ApplicationProperties.java`（修改：增加 DefaultDatabase 配置类）
- `application.yml`（修改：增加 dts.analytics.default-databases）
- `DefaultDatabaseInitService.java`（**新建**）

## 完成标准

- [ ] 配置业务库后启动，`analytics_database` 表出现对应记录
- [ ] `analytics_table` 和 `analytics_field` 中有业务库的表和字段元数据
- [ ] 重复启动不会重复创建（幂等）
- [ ] 不配置时不影响现有启动流程
