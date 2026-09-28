# Database Role Hardening Design

## Problem

`dts-copilot-analytics` 目前把数据库展示名、引擎类型和少量 host/db 启发式混在一起使用：

- 固定报表执行器默认依赖 `databaseName`，并保留 `DEFAULT_DATABASE_NAME = "园林业务库"` 的兜底。
- 默认数据库注册和“数据”页隐藏系统库，仍然依赖 `postgres + localhost/copilot-postgres + garden/copilot` 这类启发式判断。
- 历史固定报表模板在 `spec_json.queryContract.databaseName` 中硬编码了环境相关名字。

这会导致两个结果：

1. 同一个名字在不同环境里可能指向不同数据库，报表执行出现环境漂移。
2. 新环境重装后，即使业务逻辑没变，也可能重新注册出错误的“业务库”或重新暴露系统运行库。

## Goal

给 `analytics_database` 引入显式 `database_role` 模型，让固定报表执行、数据库可见性和默认注册都依赖角色而不是名字或启发式判断。要求新环境初始化后不再依赖 `园林业务库` 这类字符串。

## Chosen Approach

采用显式角色字段：

- `SYSTEM_RUNTIME`
- `BUSINESS_PRIMARY`
- `BUSINESS_SECONDARY`
- `SAMPLE`

固定报表默认绑定到 `BUSINESS_PRIMARY`。模板里历史 `queryContract.databaseName` 继续兼容读取，但只作为过渡输入，不再驱动主选择逻辑。

## Design

### 1. Database model

给 [AnalyticsDatabase.java](/opt/prod/prs/source/dts-copilot/.worktrees/database-role-hardening/dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/domain/AnalyticsDatabase.java) 增加 `databaseRole` 字段，并补一个枚举 `AnalyticsDatabaseRole`。

数据库迁移会：

- 为 `analytics_database` 增加 `database_role`
- 根据现有记录和 `details_json` 中关联的数据源信息回填角色
- 为已有 sample 库回填 `SAMPLE`
- 为运行时 Postgres 库回填 `SYSTEM_RUNTIME`
- 对其余外部业务库回填 `BUSINESS_PRIMARY` 或 `BUSINESS_SECONDARY`

### 2. Fixed report resolution

[DefaultFixedReportExecutionService.java](/opt/prod/prs/source/dts-copilot/.worktrees/database-role-hardening/dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionService.java) 去掉按 `DEFAULT_DATABASE_NAME` 解析的主路径：

- 先看模板 `queryContract.databaseRole`
- 没配时默认按 `BUSINESS_PRIMARY`
- 只有在角色缺失且模板仍带旧 `databaseName` 时，才用名字做兼容兜底

这样模板升级后，环境里就不再需要“园林业务库”这个展示名。

### 3. Database listing and init

[DatabaseResource.java](/opt/prod/prs/source/dts-copilot/.worktrees/database-role-hardening/dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/web/rest/DatabaseResource.java) 的列表和删除保护改成按 `databaseRole` 判断，而不是动态猜测系统库。

[DefaultDatabaseInitService.java](/opt/prod/prs/source/dts-copilot/.worktrees/database-role-hardening/dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/DefaultDatabaseInitService.java) 的默认注册配置增加显式 role。`application.yml` 里默认库条目不再只靠名字表达语义。

### 4. Template cleanup

新增一条前向 Liquibase：

- 把所有固定报表模板里的 `queryContract.databaseName` 清掉
- 同时给固定报表 `queryContract` 增加 `databaseRole = "BUSINESS_PRIMARY"`

这样 fresh install 和已有环境会在同一收敛路径上。

## Testing

最少需要补这 4 组回归：

- `DefaultFixedReportExecutionServiceTest`
  - 固定报表优先按 `databaseRole=BUSINESS_PRIMARY` 解析
  - 不再依赖 `园林业务库`
- `DefaultDatabaseInitServiceTest`
  - 默认注册写入正确 role
  - 运行时库不会被误注册为业务库
- `DatabaseResourceTest`
  - “数据”页隐藏 `SYSTEM_RUNTIME`
  - 业务库仍可见
- Liquibase verification test
  - 新 changeset 会为历史模板移除 `databaseName`
  - 新 changeset 会给模板写入 `databaseRole`

## Rollout

先在本地和测试环境验证 role 回填与模板升级，再部署远端。部署后需要核对：

- `analytics_database.database_role` 回填正确
- `/api/database` 不再显示运行时库
- 固定报表执行结果使用 `BUSINESS_PRIMARY`
- 新环境初始化时不再依赖 `园林业务库` 名称
