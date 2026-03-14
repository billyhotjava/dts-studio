# DTS Copilot — 独立 AI 助手 + BI 分析服务

从 dts-stack v2.4.1 独立抽取的增值服务组件，通过 API Key 认证，可低成本集成到任意业务系统。

## 架构

```
HTTPS (Traefik TLS 终止)
├─ /api/ai/*     → copilot-ai:8091       AI 引擎（LLM Gateway / ReAct Agent / NL2SQL / RAG）
├─ /api/*        → copilot-analytics:8092 BI 分析（查询 / 仪表盘 / 报表 / 大屏）
└─ /*            → copilot-webapp:80      React 前端（BI 界面 + AI 聊天面板）

基础设施
├─ PostgreSQL 16 (pgvector)   copilot_ai + copilot_analytics 两个 schema
└─ Ollama                     本地 LLM（qwen2.5-coder:7b）
```

## 技术栈

| 模块 | 技术 |
|------|------|
| copilot-ai | Java 21 + Spring Boot 3.4.5 + JPA + Liquibase |
| copilot-analytics | Java 21 + Spring Boot 3.4.5 + JDBC + JPA |
| copilot-webapp | React 19 + TypeScript + Vite 6 + Ant Design 5 |
| 反向代理 | Traefik v3.3 (TLS 终止 + 路由) |
| 数据库 | PostgreSQL 16 + pgvector (向量检索) |
| LLM | Ollama (OpenAI 兼容接口) |

---

## 快速启动

### 前提条件

- Docker 24+ & Docker Compose v2
- Java 21 + Maven 3.9+（仅开发时需要）
- Node.js 20+ & pnpm（仅前端开发时需要）

### 生产模式（全容器）

```bash
# 1. 生成 HTTPS 证书
bash services/certs/gen-certs.sh

# 2. （可选）添加本地域名解析
echo "127.0.0.1 copilot.local" | sudo tee -a /etc/hosts

# 3. 编译打包
mvn clean package -DskipTests
cd dts-copilot-webapp && pnpm install && pnpm build && cd ..

# 4. 启动所有服务
docker compose up -d --build

# 5. 等待启动完成（约 60 秒），验证健康状态
curl -fsS http://localhost:8091/actuator/health   # copilot-ai
curl -fsS http://localhost:8092/api/health         # copilot-analytics
curl -fsS http://localhost:11434/api/tags          # ollama

# 6. 通过 HTTPS 访问（使用本地 CA 证书）
curl --cacert services/certs/ca.crt https://copilot.local/
```

### 开发模式（IDE + 基础设施容器）

```bash
# 1. 只启动基础设施（PostgreSQL + Ollama）
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 2. 在 IDE 中启动 Java 服务
#    - CopilotAiApplication       (端口 8091)
#    - CopilotAnalyticsApplication (端口 8092)
#
#    环境变量：
#      PG_HOST=localhost
#      PG_PORT=5432
#      PG_DB=copilot
#      PG_USER=copilot
#      PG_PASSWORD=copilot_dev
#      OLLAMA_BASE_URL=http://localhost:11434
#      COPILOT_ADMIN_SECRET=change-me-in-production

# 3. 启动前端开发服务器
cd dts-copilot-webapp
pnpm install
pnpm dev      # http://localhost:3003
              # Vite 自动代理 /api/ai → :8091, /api → :8092
```

### 拉取默认 LLM 模型

```bash
# 首次启动后需要拉取模型（约 4GB）
docker exec dts-copilot-ollama ollama pull qwen2.5-coder:7b
```

---

## 首次使用

### 1. 生成 API Key

```bash
curl -X POST http://localhost:8091/api/auth/keys \
  -H "X-Admin-Secret: change-me-in-production" \
  -H "Content-Type: application/json" \
  -d '{"name": "my-app", "description": "我的应用"}'

# 返回示例：
# { "success": true, "data": { "id": 1, "rawKey": "cpk_a1b2c3d4...", "prefix": "cpk_a1b2" } }
# ⚠️ rawKey 仅返回一次，请妥善保存
```

### 2. 验证 AI 服务

```bash
API_KEY="cpk_a1b2c3d4..."

# 查看 AI 状态
curl -H "Authorization: Bearer $API_KEY" http://localhost:8091/api/ai/copilot/status

# SQL 补全
curl -X POST http://localhost:8091/api/ai/copilot/complete \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "查询所有用户的数量", "context": "CREATE TABLE users (id INT, name VARCHAR(100))"}'

# 自然语言转 SQL
curl -X POST http://localhost:8091/api/ai/copilot/nl2sql \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"naturalLanguage": "统计每个部门的员工数量", "schemaContext": "CREATE TABLE employee (id INT, name VARCHAR, dept_id INT); CREATE TABLE department (id INT, dept_name VARCHAR)"}'

# AI 对话
curl -X POST http://localhost:8091/api/ai/agent/chat/send \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍下你能做什么"}'
```

### 3. 注册外部数据源

```bash
curl -X POST http://localhost:8091/api/datasources \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "业务数据库",
    "dbType": "mysql",
    "jdbcUrl": "jdbc:mysql://host:3306/mydb?useUnicode=true&characterEncoding=utf8",
    "username": "readonly_user",
    "password": "xxx"
  }'
```

---

## 与园林管理平台对接

### 对接架构

```
园林平台用户 → adminweb → rs-gateway
                            │
                CopilotAuthConvertFilter
                  ├─ 从 JWT 提取用户信息
                  ├─ 注入 API Key 头
                  └─ 转发到 copilot 服务
                            │
                    ┌───────┴───────┐
              copilot-ai      copilot-analytics
```

### 第一步：启动 dts-copilot 并生成 API Key

参考上面的「快速启动」和「首次使用」章节。

### 第二步：注册园林平台 MySQL 数据源

```bash
# 先在园林 MySQL 中创建只读用户（安全起见）
mysql -u root -p -e "
  CREATE USER 'copilot_reader'@'%' IDENTIFIED BY 'your_password';
  GRANT SELECT ON flowers.* TO 'copilot_reader'@'%';
  FLUSH PRIVILEGES;
"

# 通过脚本注册
export API_KEY="cpk_xxx"
export GARDEN_DB_HOST="garden-mysql-host"
export GARDEN_DB_PASSWORD="your_password"
bash scripts/register-garden-datasource.sh
```

### 第三步：配置 rs-gateway 路由

在 Nacos 中为 rs-gateway 添加路由配置（参考 `docs/integration/gateway-routes.yml`）：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: copilot-ai
          uri: http://copilot-ai:8091
          predicates:
            - Path=/copilot/ai/**
          filters:
            - StripPrefix=2

        - id: copilot-analytics
          uri: http://copilot-analytics:8092
          predicates:
            - Path=/copilot/analytics/**
          filters:
            - StripPrefix=2

        - id: copilot-webapp
          uri: http://copilot-webapp:80
          predicates:
            - Path=/copilot/web/**
          filters:
            - StripPrefix=2
```

### 第四步：添加认证转换过滤器

将 `docs/integration/gateway-auth-filter.java` 中的 `CopilotAuthConvertFilter` 添加到 rs-gateway 模块。
该过滤器负责：
- 拦截 `/copilot/**` 路径的请求
- 注入 `Authorization: Bearer <copilot-api-key>` 头
- 将园林平台的 `X-Access-User-Id` 等头转换为 `X-DTS-User-Id` 等

在 rs-gateway 的配置中添加 API Key：
```yaml
copilot:
  api-key: cpk_xxx
```

### 第五步：adminweb 嵌入 Copilot 页面

1. 复制 `docs/integration/adminweb-copilot-view.vue` 到 `adminweb/src/views/copilot/` 目录
2. 在 adminweb router 中添加路由：
   ```js
   { path: '/copilot/chat', component: () => import('@/views/copilot/chat'), name: 'CopilotChat' }
   ```
3. 执行 `docs/integration/adminweb-menu.sql` 添加菜单项到数据库

### 第六步：验证端到端链路

```bash
# 通过 Gateway 验证（需要园林平台 JWT Token）
curl -H "Authorization: Bearer <garden-jwt-token>" \
     http://garden-gateway:7091/copilot/ai/api/ai/copilot/status

# 验证 NL2SQL（通过 Gateway）
curl -X POST http://garden-gateway:7091/copilot/ai/api/ai/copilot/nl2sql \
  -H "Authorization: Bearer <garden-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"naturalLanguage": "查询在租项目总数"}'
```

---

## 调试指南

### 日志查看

```bash
# 查看所有服务日志
docker compose logs -f

# 单独查看某个服务
docker compose logs -f copilot-ai
docker compose logs -f copilot-analytics
docker compose logs -f copilot-proxy

# 查看最近 100 行
docker compose logs --tail 100 copilot-ai
```

### IDE 调试（推荐方式）

1. 启动基础设施：
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
   ```

2. 在 IntelliJ IDEA 中：
   - 打开 `/opt/prod/prs/source/dts-copilot` 作为 Maven 项目
   - 配置 Run Configuration:
     - **CopilotAiApplication** — 端口 8091，设置环境变量 `PG_HOST=localhost`
     - **CopilotAnalyticsApplication** — 端口 8092，设置环境变量 `PG_HOST=localhost;COPILOT_AI_BASE_URL=http://localhost:8091`
   - 打断点、Debug 启动

3. 前端调试：
   ```bash
   cd dts-copilot-webapp
   pnpm dev    # 访问 http://localhost:3003，支持 HMR 热更新
   ```

### 数据库调试

```bash
# 连接到 PostgreSQL
docker exec -it dts-copilot-postgres psql -U copilot -d copilot

# 查看 schema
\dn

# 查看 copilot_ai 的表
SET search_path TO copilot_ai;
\dt

# 查看 copilot_analytics 的表
SET search_path TO copilot_analytics;
\dt

# 查看 API Key（脱敏）
SELECT id, key_prefix, name, status, created_at FROM copilot_ai.api_key;

# 查看 AI 审计日志
SELECT * FROM copilot_ai.ai_audit_log ORDER BY created_at DESC LIMIT 20;

# 查看聊天会话
SELECT * FROM copilot_ai.ai_chat_session ORDER BY created_at DESC LIMIT 10;
```

### Ollama 调试

```bash
# 查看已安装的模型
curl http://localhost:11434/api/tags

# 拉取新模型
docker exec dts-copilot-ollama ollama pull qwen2.5:14b

# 直接测试模型
curl -X POST http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5-coder:7b",
    "messages": [{"role": "user", "content": "SELECT 语句的基本语法是什么？"}]
  }'

# 查看 Ollama 资源占用
docker stats dts-copilot-ollama
```

### HTTPS / 证书调试

```bash
# 重新生成证书
rm -f services/certs/*.crt services/certs/*.key services/certs/*.p12
bash services/certs/gen-certs.sh

# 验证证书
openssl x509 -in services/certs/server.crt -noout -subject -dates
openssl x509 -in services/certs/server.crt -noout -ext subjectAltName

# 验证证书链
openssl verify -CAfile services/certs/ca.crt services/certs/server.only.crt

# 测试 HTTPS 连接
curl -v --cacert services/certs/ca.crt https://copilot.local/

# 信任本地 CA（消除浏览器警告）
# macOS:
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain services/certs/ca.crt
# Ubuntu:
sudo cp services/certs/ca.crt /usr/local/share/ca-certificates/copilot-ca.crt
sudo update-ca-certificates
```

### Traefik 路由调试

```bash
# 查看 Traefik 日志
docker compose logs copilot-proxy

# 临时开启 Traefik Dashboard（仅调试用）
# 修改 services/proxy/traefik.yml:
#   api:
#     dashboard: true
#     insecure: true
# 然后访问 http://localhost:8080/dashboard/

# 验证路由规则
curl --cacert services/certs/ca.crt https://copilot.local/api/ai/copilot/status  # → copilot-ai
curl --cacert services/certs/ca.crt https://copilot.local/api/health              # → copilot-analytics
curl --cacert services/certs/ca.crt https://copilot.local/                        # → copilot-webapp
```

### 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| copilot-ai 启动失败 | Liquibase 迁移失败，通常是 pgvector 扩展未安装 | 确认使用 `pgvector/pgvector:pg16` 镜像 |
| Ollama 返回空 | 模型未拉取 | `docker exec dts-copilot-ollama ollama pull qwen2.5-coder:7b` |
| API 返回 401 | API Key 无效或未传递 | 检查 Authorization 头格式: `Bearer cpk_xxx` |
| NL2SQL 结果不准 | 上下文不足 | 在请求中提供 schemaContext（DDL 语句） |
| 前端 CORS 错误 | 直接访问后端端口 | 使用 Vite proxy（开发）或 Traefik/Nginx（生产） |
| iframe 白屏 | X-Frame-Options 限制 | Traefik 已配置 `frameDeny: false`，检查业务系统 CSP |
| 数据源连接失败 | 网络不通或权限不足 | 确认 copilot 容器可访问目标数据库，检查用户 SELECT 权限 |

---

## API 端点速查

### copilot-ai (端口 8091)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/copilot/complete` | SQL 补全 |
| POST | `/api/ai/copilot/stream` | 流式补全 (SSE) |
| POST | `/api/ai/copilot/nl2sql` | 自然语言转 SQL |
| POST | `/api/ai/copilot/explain` | SQL 解释 |
| POST | `/api/ai/copilot/optimize` | SQL 优化 |
| GET | `/api/ai/copilot/status` | AI 状态 |
| POST | `/api/ai/agent/chat/send` | 发送对话消息 |
| POST | `/api/ai/agent/chat/stream` | 流式对话 (SSE) |
| GET | `/api/ai/agent/chat/sessions` | 会话列表 |
| GET | `/api/ai/agent/chat/{sessionId}` | 会话详情 |
| DELETE | `/api/ai/agent/chat/{sessionId}` | 删除会话 |
| GET/POST/PUT/DELETE | `/api/ai/config/providers` | AI Provider 管理 |
| POST | `/api/auth/keys` | 生成 API Key |
| GET | `/api/auth/keys` | 列出 API Key |
| DELETE | `/api/auth/keys/{id}` | 吊销 API Key |

### copilot-analytics (端口 8092)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| GET/POST/PUT/DELETE | `/api/databases` | 数据源管理 |
| GET/POST/PUT/DELETE | `/api/cards` | 查询卡片管理 |
| POST | `/api/cards/{id}/execute` | 执行查询 |
| GET/POST/PUT/DELETE | `/api/dashboards` | 仪表盘管理 |
| GET/POST/PUT/DELETE | `/api/screens` | 大屏管理 |
| GET/POST/PUT/DELETE | `/api/collections` | 集合管理 |
| GET | `/api/public/card/{id}` | 公开卡片 (无需认证) |
| GET | `/api/public/dashboard/{id}` | 公开仪表盘 (无需认证) |
| GET | `/api/public/screen/{id}` | 公开大屏 (无需认证) |

---

## 目录结构

```
dts-copilot/
├── pom.xml                          Maven parent
├── docker-compose.yml               生产编排
├── docker-compose.dev.yml           开发模式（仅基础设施）
├── .env                             环境变量
├── build.sh                         编译打包脚本
├── smoke-test.sh                    冒烟测试
│
├── dts-copilot-ai/                  AI 引擎服务 (8091)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/.../ai/
│       │   ├── config/              Spring Security 配置
│       │   ├── domain/              JPA 实体
│       │   ├── health/              Ollama 健康检查
│       │   ├── repository/          数据访问
│       │   ├── security/            API Key 认证过滤器
│       │   ├── service/
│       │   │   ├── agent/           ReAct Agent 引擎
│       │   │   ├── audit/           操作审计
│       │   │   ├── auth/            API Key 管理
│       │   │   ├── chat/            对话会话管理
│       │   │   ├── config/          AI Provider 配置
│       │   │   ├── copilot/         Copilot 核心（NL2SQL/补全）
│       │   │   ├── llm/             LLM 客户端 + Gateway
│       │   │   ├── rag/             RAG 向量检索
│       │   │   ├── safety/          SQL 沙箱 + 安全防护
│       │   │   └── tool/            Tool 注册 + 内置/业务 Tool
│       │   └── web/rest/            REST 控制器
│       └── resources/
│           ├── application.yml
│           └── config/liquibase/    数据库迁移脚本
│
├── dts-copilot-analytics/           BI 分析服务 (8092)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/.../analytics/
│       │   ├── domain/              BI 实体（Dashboard/Card/Screen...）
│       │   ├── repository/          数据访问
│       │   ├── security/            认证（通过 copilot-ai 验证）
│       │   ├── service/             业务逻辑 + 查询执行
│       │   └── web/rest/            REST 控制器
│       └── resources/
│           ├── application.yml
│           └── config/liquibase/
│
├── dts-copilot-webapp/              React 前端 (3003/80)
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── components/ai/           AI 聊天面板
│       ├── layouts/                 主布局 + 嵌入布局
│       └── pages/                   页面组件
│
├── services/
│   ├── certs/                       证书管理
│   │   └── gen-certs.sh             证书生成脚本
│   └── proxy/                       Traefik 配置
│       ├── traefik.yml
│       └── dynamic/routes.yml
│
├── scripts/                         运维脚本
│   └── register-garden-datasource.sh
│
├── docs/integration/                集成文档与示例
│   ├── README.md
│   ├── gateway-routes.yml
│   ├── gateway-auth-filter.java
│   ├── adminweb-copilot-view.vue
│   └── adminweb-menu.sql
│
└── worklog/v1.0.0/                  Sprint 规划与任务追踪
    ├── sprint-1/ ~ sprint-7/
    └── sprint-queue.md
```
