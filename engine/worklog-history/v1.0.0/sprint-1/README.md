# Sprint-1: 项目脚手架与基础设施 (SC)

**前缀**: SC (Scaffold)
**状态**: READY
**目标**: 搭建 dts-copilot 双服务 Maven 项目骨架、PostgreSQL 双 schema 基线、Docker Compose 编排、Traefik HTTPS 反向代理与证书管理，确保空项目可编译、可启动、可通过 HTTPS 访问、可健康检查。

## 背景

dts-copilot 是从 dts-stack v2.4.1 独立出来的增值服务，采用双服务架构：
- `dts-copilot-ai` (8091): AI 引擎，源自 dts-platform 的 AI 模块
- `dts-copilot-analytics` (8092): BI 分析，源自 dts-analytics

两个服务共用一个 PostgreSQL 实例，使用不同 schema（`copilot_ai` / `copilot_analytics`）。
本 sprint 完成项目基础设施搭建，为后续代码抽取提供骨架。

## 技术决策

- Spring Boot 3.4.5（与 dts-stack 保持一致）
- Java 21
- Maven 多模块（parent pom + copilot-ai + copilot-analytics）
- Liquibase schema 管理
- Docker Compose 编排（postgres + ollama + traefik + copilot-ai + copilot-analytics + webapp）
- Traefik 反向代理 + TLS 终止（开发自签名 / 生产外部证书）
- 证书管理：gen-certs.sh 自动生成本地 CA + 通配符证书（复用 dts-stack 方案）
- 包名: `com.yuzhi.dts.copilot.ai` / `com.yuzhi.dts.copilot.analytics`

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| SC-01 | Maven 多模块项目骨架搭建 | READY | - |
| SC-02 | copilot_ai schema Liquibase 基线 | READY | SC-01 |
| SC-03 | copilot_analytics schema Liquibase 基线 | READY | SC-01 |
| SC-04 | Docker Compose 编排 | READY | SC-01 |
| SC-05 | Ollama 容器集成与健康检查 | READY | SC-04 |
| SC-06 | 证书生成脚本与本地 CA | READY | - |
| SC-07 | Traefik 反向代理与 TLS 终止 | READY | SC-04, SC-06 |
| SC-08 | CI 构建脚本与冒烟验证 | READY | SC-01~07 |

## 完成标准

- [ ] `mvn clean compile` 两个子模块均编译通过
- [ ] `docker compose up -d` 所有容器启动且健康检查通过
- [ ] copilot-ai 8091 端口 `/actuator/health` 返回 UP
- [ ] copilot-analytics 8092 端口 `/api/health` 返回正常
- [ ] PostgreSQL 中 `copilot_ai` 和 `copilot_analytics` 两个 schema 创建成功
- [ ] Ollama 容器启动并可响应 `/api/tags` 请求
- [ ] `gen-certs.sh` 生成本地 CA + 通配符服务器证书 + truststore
- [ ] Traefik 容器启动，HTTPS 443 端口可访问
- [ ] `https://copilot.local/api/ai/copilot/status` 通过 Traefik 路由到 copilot-ai
- [ ] `https://copilot.local/analytics/` 通过 Traefik 路由到 copilot-webapp
- [ ] HTTP 80 自动重定向到 HTTPS 443

## IT 验证命令

```bash
# 编译验证
cd dts-copilot && mvn clean compile

# 启动所有服务
cd dts-copilot && docker compose up -d

# 健康检查
curl -fsS http://localhost:8091/actuator/health
curl -fsS http://localhost:8092/api/health

# 数据库 schema 验证
docker exec dts-copilot-postgres psql -U copilot -d copilot -c "\dn"

# Ollama 验证
curl -fsS http://localhost:11434/api/tags

# 证书生成
cd dts-copilot && bash services/certs/gen-certs.sh

# HTTPS 验证（使用生成的 CA 证书信任）
curl --cacert services/certs/ca.crt https://copilot.local/api/ai/copilot/status
curl --cacert services/certs/ca.crt https://copilot.local/analytics/

# HTTP → HTTPS 重定向验证
curl -v http://copilot.local/  # 预期 301 → https://
```

## 优先级说明

SC-01 最先 → SC-02/SC-03/SC-04/SC-06 可并行 → SC-05 依赖 SC-04, SC-07 依赖 SC-04+SC-06 → SC-08 收尾
