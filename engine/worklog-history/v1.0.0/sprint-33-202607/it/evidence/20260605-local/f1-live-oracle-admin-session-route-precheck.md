# Sprint-33 F1 live oracle admin session route precheck

**时间**: 2026-06-05
**环境**: local docker compose (`dts-stack-dts-admin-1`, `dts-copilot`)
**结论**: BLOCKED FOR LIVE ROUTE。admin 标准登录链路已可换取有效 session token，但当前运行的 `dts-stack-dts-admin-1` 不是 legacy `adminapi/rs-gateway` 业务入口，`/rs-flowers-base/...` L2 oracle 路径认证后仍被 Spring Security 拒绝。

## 预检结果

admin 登录链路：

```text
POST http://127.0.0.1:38012/api/keycloak/auth/login
HTTP 200
accessToken=present
refreshToken=present
user=sysadmin
rolesCount=4
```

普通 admin API：

```text
GET /api/keycloak/users
HTTP 200

GET /api/admin/users
HTTP 200
```

legacy L2 oracle 路径：

```text
GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage?projectId=1001&bizCode=BX202606030968
Authorization: Bearer <admin-session-token>
HTTP 403
WWW-Authenticate: Bearer error="insufficient_scope"
```

路径对照：

```text
/rs-flowers-base/...      -> HTTP 403
/api/rs-flowers-base/...  -> HTTP 404
/admin/rs-flowers-base/...-> HTTP 404
```

权限更新后追加复核：

```text
docker ps
no rs-gateway container
no rs-flowers-base / flowerbase container

dts-stack-dts-admin-1 labels
PathPrefix(`/api`)
PathPrefix(`/admin/api`)
PathPrefix(`/v3/api-docs`)
no PathPrefix(`/rs-flowers-base`)
```

legacy adminapi 启动面：

```text
adminapi/docker/docker-compose.yml
gateway -> 7091, depends_on nacos
flowerbase -> 7095, depends_on nacos
web -> publishes 8000:80 and proxies /flowers-dev-api to gateway:7091

current host
port 8000 is already occupied by portainer
```

## 判断

- 无凭证 401 已不是唯一问题；admin session 已可生成并访问 `/api/**` governance API。
- `SaleAccountController.listSaleAccountPage` 存在于 legacy `adminapi/rs-modules/rs-flowers-base`，不是当前 `dts-stack/source/dts-admin` 的 `/api/**` 资源。
- 当前 Docker 栈没有运行 legacy `rs-gateway` 或 `rs-flowers-base` 服务，`dts-admin` 的 Traefik 路由标签也没有发布 `/rs-flowers-base` 前缀。
- legacy 入口需要 gateway + flowerbase + Nacos/Redis/配置链一起成立；当前 host 上 compose 的 web 端口 `8000` 已被占用，不能无审查地整套启动。
- 当前 `dts-stack-dts-admin-1` 只证明 admin/OIDC 权限链路可用；F1 live oracle 仍需要真实 legacy `adminapi/rs-gateway` 或 `rs-flowers-base` base URL。

## 下一步所需

- 提供或启动 legacy `adminapi` 业务入口，并确认其 `/flowers-dev-api/rs-flowers-base/...` 或 gateway 等价路径可访问。
- 将该 base URL 配置到 `FINANCE_RECONCILIATION_ORACLE_BASE_URL`，再与 analytics `/api/dataset` 双路重跑 F1 harness。
