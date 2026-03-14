# SC-07: Traefik 反向代理与 TLS 终止

**状态**: READY
**依赖**: SC-04, SC-06

## 目标

配置 Traefik 作为 dts-copilot 的统一入口，提供 HTTPS TLS 终止、路由分发、HTTP→HTTPS 重定向和安全头。

## 技术设计

### 服务拓扑

```
客户端
  ↓ HTTPS (443)
┌─ Traefik ──────────────────────────────┐
│  TLS 终止 + 路由 + 安全头               │
│                                         │
│  /api/ai/*     → copilot-ai:8091       │
│  /analytics/*  → copilot-webapp:80     │
│  /api/*        → copilot-analytics:8092 │
│  /             → copilot-webapp:80     │
└─────────────────────────────────────────┘
  ↓ HTTP (内部)
后端服务（无需各自配 TLS）
```

### Traefik 静态配置

```yaml
# services/proxy/traefik.yml
api:
  dashboard: false

entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure
          scheme: https
  websecure:
    address: ":443"

providers:
  file:
    directory: /etc/traefik/dynamic
    watch: true
```

### Traefik 动态配置

```yaml
# services/proxy/dynamic/routes.yml
http:
  routers:
    copilot-ai:
      rule: "PathPrefix(`/api/ai`)"
      service: copilot-ai
      entryPoints: [websecure]
      tls: {}
      priority: 20

    copilot-analytics-api:
      rule: "PathPrefix(`/api`)"
      service: copilot-analytics
      entryPoints: [websecure]
      tls: {}
      priority: 10

    copilot-webapp:
      rule: "PathPrefix(`/`)"
      service: copilot-webapp
      entryPoints: [websecure]
      tls: {}
      priority: 1

  services:
    copilot-ai:
      loadBalancer:
        servers:
          - url: "http://copilot-ai:8091"

    copilot-analytics:
      loadBalancer:
        servers:
          - url: "http://copilot-analytics:8092"

    copilot-webapp:
      loadBalancer:
        servers:
          - url: "http://copilot-webapp:80"

tls:
  certificates:
    - certFile: /certs/server.crt
      keyFile: /certs/server.key
  options:
    default:
      minVersion: VersionTLS12
      sniStrict: false  # 开发模式允许 IP 直连

  stores:
    default:
      defaultCertificate:
        certFile: /certs/server.crt
        keyFile: /certs/server.key
```

### Docker Compose 服务

```yaml
copilot-proxy:
  image: traefik:v3.3
  ports:
    - "${TLS_PORT:-443}:443"
    - "${HTTP_PORT:-80}:80"
  volumes:
    - ./services/proxy/traefik.yml:/etc/traefik/traefik.yml:ro
    - ./services/proxy/dynamic:/etc/traefik/dynamic:ro
    - ./services/certs/server.crt:/certs/server.crt:ro
    - ./services/certs/server.key:/certs/server.key:ro
  depends_on:
    - copilot-ai
    - copilot-analytics
    - copilot-webapp
  healthcheck:
    test: ["CMD", "traefik", "healthcheck"]
    interval: 30s
    timeout: 10s
    retries: 3
  restart: unless-stopped
```

### 路由优先级说明

| 优先级 | 路径 | 目标服务 | 说明 |
|--------|------|---------|------|
| 20 | `/api/ai/*` | copilot-ai:8091 | AI 引擎 API（优先匹配） |
| 10 | `/api/*` | copilot-analytics:8092 | BI 分析 API |
| 1 | `/*` | copilot-webapp:80 | 前端静态资源（兜底） |

`/api/ai/` 优先级高于 `/api/`，确保 AI 请求不会误路由到 analytics。

### 安全头中间件

```yaml
http:
  middlewares:
    security-headers:
      headers:
        stsSeconds: 31536000          # HSTS 1 年
        stsIncludeSubdomains: true
        contentTypeNosniff: true
        browserXssFilter: true
        frameDeny: false              # 允许 iframe 嵌入（业务需要）
        customFrameOptionsValue: "SAMEORIGIN"
```

### 开发模式禁用 Traefik（可选）

在 `docker-compose.dev.yml` 中可排除 Traefik，直接通过内部端口访问后端：
```yaml
services:
  copilot-proxy:
    profiles: ["proxy"]  # 仅在 --profile proxy 时启动
```

开发时 `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d` 不启动 Traefik，直接用 `http://localhost:8091`。

### 与园林平台集成时的 TLS 策略

当 dts-copilot 部署在园林平台 rs-gateway 后面时，有两种选择：

1. **关闭 Traefik**（推荐）：Garden Gateway 做 TLS 终止，copilot 走内部 HTTP
2. **保留 Traefik**：Garden Gateway 转发到 Traefik HTTPS 端口，端到端加密

通过环境变量 `COPILOT_TLS_ENABLED=true/false` 控制。

## 影响文件

- `dts-copilot/services/proxy/traefik.yml`（新建）
- `dts-copilot/services/proxy/dynamic/routes.yml`（新建）
- `dts-copilot/docker-compose.yml`（修改：添加 copilot-proxy 服务）
- `dts-copilot/.env`（修改：添加 TLS_PORT、HTTP_PORT、COPILOT_TLS_ENABLED）

## 完成标准

- [ ] Traefik 容器启动且健康检查通过
- [ ] `https://copilot.local/api/ai/copilot/status` 正确路由到 copilot-ai
- [ ] `https://copilot.local/api/health` 正确路由到 copilot-analytics
- [ ] `https://copilot.local/` 正确路由到 copilot-webapp
- [ ] `http://copilot.local/` 自动 301 重定向到 HTTPS
- [ ] TLS 版本最低 1.2
- [ ] 安全头正确设置（HSTS、XSS Filter、Content-Type nosniff）
- [ ] 开发模式可通过 profile 跳过 Traefik
