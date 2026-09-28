# SC-02: copilot_ai schema Liquibase 基线

**状态**: READY
**依赖**: SC-01

## 目标

为 copilot-ai 服务创建 PostgreSQL `copilot_ai` schema 的 Liquibase 基线迁移，包含 AI 核心所需的基础表结构。

## 技术设计

### Liquibase 基线迁移

```sql
-- 创建 schema
CREATE SCHEMA IF NOT EXISTS copilot_ai;

-- AI 配置表
CREATE TABLE copilot_ai.ai_provider_config (
    id              BIGSERIAL PRIMARY KEY,
    provider_name   VARCHAR(64) NOT NULL,
    base_url        VARCHAR(512) NOT NULL,
    api_key         VARCHAR(512),
    model           VARCHAR(128) NOT NULL,
    is_default      BOOLEAN DEFAULT false,
    max_tokens      INTEGER DEFAULT 4096,
    temperature     DECIMAL(3,2) DEFAULT 0.7,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- API Key 管理表（sprint-4 填充完整字段，此处预建骨架）
CREATE TABLE copilot_ai.api_key (
    id              BIGSERIAL PRIMARY KEY,
    key_hash        VARCHAR(128) NOT NULL UNIQUE,
    key_prefix      VARCHAR(16) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(16) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT NOW(),
    expires_at      TIMESTAMP
);

-- 数据源注册表
CREATE TABLE copilot_ai.data_source (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    db_type         VARCHAR(32) NOT NULL,
    jdbc_url        VARCHAR(512) NOT NULL,
    username        VARCHAR(128),
    password        VARCHAR(512),
    status          VARCHAR(16) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

### Schema 隔离策略

- copilot-ai 的 Liquibase `defaultSchemaName` 设为 `copilot_ai`
- 通过 `spring.liquibase.default-schema=copilot_ai` 配置
- JPA 实体使用 `@Table(schema = "copilot_ai")` 注解

## 影响文件

- `dts-copilot-ai/src/main/resources/config/liquibase/master.xml`（新建）
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_001__baseline.xml`（新建）
- `dts-copilot-ai/src/main/resources/application.yml`（修改：添加 Liquibase 配置）

## 完成标准

- [ ] Liquibase 迁移执行成功
- [ ] `copilot_ai` schema 存在：`SELECT schema_name FROM information_schema.schemata WHERE schema_name='copilot_ai'`
- [ ] 三张基础表创建成功：`\dt copilot_ai.*`
- [ ] Liquibase changelog 锁表正常
