# BA-02: 数据源管理独立化

**状态**: READY
**依赖**: BA-01, AE-06

## 目标

将 copilot-analytics 的数据源管理从依赖 dts-platform 改为从 copilot-ai 获取，或直接内置管理。

## 技术设计

### 原来的依赖

```java
// PlatformInfraClient → dts-platform:/api/infra/data-sources
// analytics 不自己管理数据源，全部从 platform 拉取
```

### 新方案

copilot-ai 已有 `data_source` 表（SC-02），copilot-analytics 通过 REST 调用 copilot-ai 获取数据源信息：

```
copilot-analytics → copilot-ai: GET /api/datasources
copilot-analytics → copilot-ai: GET /api/datasources/{id}
```

同时 copilot-analytics 自身保留 `analytics_database` 表用于存储元数据缓存。

### copilot-ai 数据源 API

```
GET    /api/datasources           — 列出数据源
POST   /api/datasources           — 注册数据源
PUT    /api/datasources/{id}      — 修改数据源
DELETE /api/datasources/{id}      — 删除数据源
POST   /api/datasources/{id}/test — 测试连通性
GET    /api/datasources/{id}/tables — 获取表列表
GET    /api/datasources/{id}/tables/{table}/columns — 获取列列表
```

## 影响文件

- `dts-copilot-ai/src/main/java/.../service/datasource/DataSourceService.java`（新建）
- `dts-copilot-ai/src/main/java/.../web/rest/DataSourceResource.java`（新建）
- `dts-copilot-analytics/src/main/java/.../service/CopilotAiClient.java`（新建，替换 PlatformInfraClient）

## 完成标准

- [ ] copilot-ai 数据源 CRUD API 正常工作
- [ ] copilot-analytics 通过 copilot-ai API 获取数据源信息
- [ ] 数据源连通性测试可用
- [ ] 表和列元数据获取正常
