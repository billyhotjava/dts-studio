# NV-07: 同义词字典可配置化

**状态**: READY
**依赖**: NV-03

## 目标

将 `Nl2SqlSemanticRecallService` 中硬编码的同义词字典改为数据库可配置，便于运行时维护和扩展。

## 技术设计

### 当前实现

`Nl2SqlSemanticRecallService` 内置了中文 → 字段名的同义词映射（如 "产量" → "output_qty"）。硬编码在 Java 代码中，修改需重新编译部署。

### 改造方案

1. 新建 `analytics_synonym` 表（或复用 `analytics_setting` 存 JSON）：

```sql
CREATE TABLE copilot_analytics.analytics_synonym (
    id BIGSERIAL PRIMARY KEY,
    term VARCHAR(100) NOT NULL,          -- 业务术语（如"产量"）
    column_name VARCHAR(100) NOT NULL,   -- 映射的字段名（如"output_qty"）
    table_hint VARCHAR(200),             -- 可选：限定表名
    database_id BIGINT,                  -- 可选：限定数据源
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX ux_synonym_term_col ON copilot_analytics.analytics_synonym(term, column_name);
```

2. `Nl2SqlSemanticRecallService` 启动时从数据库加载同义词，缓存到内存
3. 提供简单的 REST API（CRUD）用于管理同义词
4. 保留硬编码的通用同义词作为 fallback

### API 端点

- `GET /api/synonyms` — 列出所有同义词
- `POST /api/synonyms` — 添加同义词
- `DELETE /api/synonyms/{id}` — 删除同义词

## 影响文件

- Liquibase migration（**新建**：`analytics_synonym` 表）
- `AnalyticsSynonym.java`（**新建**：实体）
- `AnalyticsSynonymRepository.java`（**新建**）
- `Nl2SqlSemanticRecallService.java`（修改：从 DB 加载同义词）
- `SynonymResource.java`（**新建**：REST API）

## 完成标准

- [ ] 通过 API 添加同义词后，NL2SQL 召回能匹配到对应字段
- [ ] 不影响现有硬编码同义词
- [ ] 重启后同义词仍然生效
