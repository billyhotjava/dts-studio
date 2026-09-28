# SC-03: copilot_analytics schema Liquibase 基线

**状态**: READY
**依赖**: SC-01

## 目标

为 copilot-analytics 服务创建 PostgreSQL `copilot_analytics` schema 的 Liquibase 基线迁移。此 schema 源自 dts-analytics 的数据库结构，包含 BI 分析所需的核心表。

## 技术设计

### 迁移策略

从 dts-analytics 的 Liquibase changelog 中提取核心表定义，重新组织为 copilot_analytics schema 下的基线迁移。

### 核心表（来源 dts-analytics）

- `analytics_user` — 用户信息（自动从 API Key 身份创建）
- `analytics_session` — 用户会话
- `analytics_database` — 注册的数据源连接
- `analytics_table` — 表元数据
- `analytics_field` — 字段元数据
- `analytics_card` — 查询卡片（问题/可视化）
- `analytics_dashboard` — 仪表盘
- `analytics_dashboard_card` — 仪表盘-卡片关联
- `analytics_collection` — 集合/文件夹
- `analytics_screen` — 大屏/屏幕
- `analytics_screen_version` — 屏幕版本
- `analytics_permissions_graph` — 权限图
- `analytics_audit_log` — 操作审计
- `analytics_setting` — 系统设置

### Schema 隔离

- `spring.liquibase.default-schema=copilot_analytics`
- 与 copilot_ai 共用同一 PostgreSQL 实例

## 影响文件

- `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml`（新建）
- `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/v1_0_0_001__baseline.xml`（新建）
- `dts-copilot-analytics/src/main/resources/application.yml`（修改）

## 完成标准

- [ ] Liquibase 迁移执行成功
- [ ] `copilot_analytics` schema 存在
- [ ] 核心表创建成功：`\dt copilot_analytics.*`
- [ ] copilot_ai 和 copilot_analytics 两个 schema 在同一数据库中共存
