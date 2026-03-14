# SC-04: Docker Compose 编排

**状态**: READY
**依赖**: SC-01

## 目标

创建 dts-copilot 的 Docker Compose 编排文件，实现一键启动所有服务。

## 技术设计

### 服务拓扑

```yaml
services:
  copilot-postgres:
    image: pgvector/pgvector:pg16
    ports: ["5432:5432"]
    # 创建数据库和两个 schema

  copilot-ollama:
    image: ollama/ollama:latest
    ports: ["11434:11434"]
    # GPU passthrough 可选

  copilot-ai:
    build: ./dts-copilot-ai
    ports: ["8091:8091"]
    depends_on: [copilot-postgres, copilot-ollama]

  copilot-analytics:
    build: ./dts-copilot-analytics
    ports: ["8092:8092"]
    depends_on: [copilot-postgres, copilot-ai]

  copilot-webapp:
    build: ./dts-copilot-webapp
    ports: ["3003:80"]
    depends_on: [copilot-ai, copilot-analytics]
```

### PostgreSQL 初始化脚本

```sql
-- init-db.sql
CREATE DATABASE copilot;
\c copilot
CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS copilot_ai;
CREATE SCHEMA IF NOT EXISTS copilot_analytics;
```

### 环境变量

```env
# .env
PG_HOST=copilot-postgres
PG_PORT=5432
PG_DB=copilot
PG_USER=copilot
PG_PASSWORD=copilot_dev

COPILOT_AI_PORT=8091
COPILOT_ANALYTICS_PORT=8092
COPILOT_WEBAPP_PORT=3003

OLLAMA_HOST=copilot-ollama
OLLAMA_PORT=11434
```

### 开发模式

提供 `docker-compose.dev.yml` 覆盖文件：
- postgres + ollama 容器化运行
- copilot-ai + copilot-analytics 在 IDE 中运行（排除）
- webapp 在本地 Vite dev server 运行（排除）

## 影响文件

- `dts-copilot/docker-compose.yml`（新建）
- `dts-copilot/docker-compose.dev.yml`（新建）
- `dts-copilot/.env`（新建）
- `dts-copilot/docker/postgres/init-db.sql`（新建）
- `dts-copilot/dts-copilot-ai/Dockerfile`（新建）
- `dts-copilot/dts-copilot-analytics/Dockerfile`（新建）

## 完成标准

- [ ] `docker compose up -d` 所有容器启动
- [ ] `docker compose ps` 显示所有服务 running/healthy
- [ ] PostgreSQL 两个 schema 初始化成功
- [ ] 开发模式 `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d` 只启动基础设施
