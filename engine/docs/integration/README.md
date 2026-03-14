# DTS Copilot 园林平台集成指南

本指南说明如何将 dts-copilot 集成到馨懿诚园林管理平台（adminapi + adminweb + app）。

## 架构概览

```
┌─────────────────────────────────────────────────────┐
│               园林平台 (rs-gateway)                   │
│                                                     │
│  adminweb ──► /copilot/web/**  ──► copilot-webapp   │
│  adminweb ──► /copilot/ai/**   ──► copilot-ai       │
│  adminweb ──► /copilot/analytics/** ──► copilot-analytics │
│                                                     │
│         CopilotAuthConvertFilter                    │
│         JWT → API Key + 用户信息头                    │
└─────────────────────────────────────────────────────┘
```

- **copilot-ai** (端口 8091)：AI 对话引擎，Tool 调用，数据源管理
- **copilot-analytics** (端口 8092)：BI 分析，报表，大屏数据
- **copilot-webapp** (端口 80)：前端 SPA，嵌入 iframe 或独立访问

## 快速开始

### 1. 启动 dts-copilot

```bash
cd dts-copilot
docker compose up -d
```

### 2. 生成 API Key

```bash
curl -X POST http://localhost:8091/api/auth/keys \
  -H "X-Admin-Secret: change-me-in-production" \
  -H "Content-Type: application/json" \
  -d '{"name": "garden-platform", "description": "园林平台集成"}'
```

记录返回的 `apiKey` 值（格式：`cpk_xxx`），后续配置需要。

### 3. 注册园林平台数据源

```bash
export API_KEY=cpk_xxx
export GARDEN_DB_HOST=your-mysql-host
export GARDEN_DB_PASSWORD=your-password
./scripts/register-garden-datasource.sh
```

### 4. 配置 Gateway 路由

将 `docs/integration/gateway-routes.yml` 中的路由配置添加到 rs-gateway 的 Nacos 配置中。

### 5. 配置认证转换

将 `docs/integration/gateway-auth-filter.java` 中的过滤器添加到 rs-gateway 模块，并在 application.yml 中配置：

```yaml
copilot:
  api-key: cpk_xxx  # 第 2 步生成的 API Key
```

### 6. 嵌入前端页面

- 将 `docs/integration/adminweb-copilot-view.vue` 复制到 `adminweb/src/views/copilot/chat.vue`
- 执行 `docs/integration/adminweb-menu.sql` 添加菜单项
- 在 router 中添加对应路由

## 认证流程

```
用户请求 → rs-gateway
  ├── JWT 验证（园林平台 AuthFilter）
  ├── 提取用户信息到 X-Access-* 请求头
  └── CopilotAuthConvertFilter
        ├── 替换 Authorization: Bearer <copilot-api-key>
        ├── 设置 X-DTS-User-Id
        ├── 设置 X-DTS-User-Name
        └── 设置 X-DTS-Display-Name
              └── 转发到 copilot-ai / copilot-analytics
```

## API 参考

### copilot-ai 核心接口

| 方法   | 路径                    | 说明             |
|--------|------------------------|------------------|
| POST   | /api/chat              | 发送对话消息       |
| GET    | /api/chat/sessions     | 获取会话列表       |
| POST   | /api/datasources       | 注册数据源         |
| GET    | /api/datasources       | 获取数据源列表     |
| POST   | /api/auth/keys         | 生成 API Key      |

### copilot-analytics 核心接口

| 方法   | 路径                    | 说明             |
|--------|------------------------|------------------|
| POST   | /api/reports           | 创建报表           |
| GET    | /api/reports           | 获取报表列表       |
| GET    | /api/dashboards        | 获取仪表盘数据     |

## 自定义 Tool 开发

dts-copilot 支持通过实现 `CopilotTool` 接口来开发自定义业务工具。

### 步骤

1. 创建 Java 类，实现 `CopilotTool` 接口
2. 添加 `@Component` 注解，Spring 会自动注册到 `ToolRegistry`
3. 使用 `SqlSandbox` 验证所有 SQL 查询

### 示例

参考已有的园林业务工具：

- `GardenProjectQueryTool` - 项目查询（带条件筛选）
- `FlowerStatsTool` - 花卉统计（多维度聚合）
- `FinanceSummaryTool` - 财务摘要（多报表类型）

### 关键要点

```java
@Component
public class MyCustomTool implements CopilotTool {

    private final SqlSandbox sqlSandbox;
    private final DataSource dataSource;

    // 构造注入 SqlSandbox 和 DataSource
    public MyCustomTool(SqlSandbox sqlSandbox, DataSource dataSource) {
        this.sqlSandbox = sqlSandbox;
        this.dataSource = dataSource;
    }

    // 所有 SQL 必须通过 SqlSandbox 验证
    // SqlSandbox 仅允许 SELECT/WITH/EXPLAIN/SHOW
    // 自动拦截 INSERT/UPDATE/DELETE/DROP 等危险操作
}
```

## 故障排查

### Gateway 路由不生效

1. 确认 Nacos 配置已刷新：检查 rs-gateway 日志中是否加载了 copilot 路由
2. 确认 copilot 服务网络可达：`curl http://copilot-ai:8091/actuator/health`
3. 检查路径前缀：gateway 会 strip 前两级路径（/copilot/ai/），确认目标服务路由

### 认证失败 (401)

1. 检查 API Key 是否有效：`curl -H "Authorization: Bearer cpk_xxx" http://localhost:8091/api/chat/sessions`
2. 确认 CopilotAuthConvertFilter 已加载：检查 gateway 启动日志
3. 确认 copilot.api-key 配置正确

### 数据源连接失败

1. 检查网络连通性：copilot 容器是否能访问园林平台 MySQL
2. 确认用户权限：copilot_reader 用户是否有 SELECT 权限
3. 检查连接参数：字符集、SSL 设置

### iframe 嵌入白屏

1. 检查浏览器控制台：是否有 CSP 或 X-Frame-Options 错误
2. 确认 copilot-webapp 允许 iframe 嵌入
3. 检查 postMessage origin 设置

### Tool 执行报错

1. 检查 copilot-ai 日志中的具体错误
2. 确认目标表存在于已注册的数据源中
3. SQL 被 SqlSandbox 拦截时会返回具体原因
