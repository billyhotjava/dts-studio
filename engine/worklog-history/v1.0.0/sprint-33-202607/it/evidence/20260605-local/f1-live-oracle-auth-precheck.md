# Sprint-33 F1 live oracle auth precheck

**时间**: 2026-06-05
**环境**: local docker compose (`dts-stack-dts-admin-1`, `dts-copilot-analytics`)
**结论**: BLOCKED FOR LIVE。F1 明细对账 harness 的本地 contract 已 PASS，但真实 L2 adminapi oracle 端点仍需要有效用户/JWT 会话，当前不能用无凭证、服务头或 test token 作为财务 oracle 证据。

## 预检结果

`dts-stack-dts-admin-1` 健康:

```text
GET http://127.0.0.1:38012/management/health
HTTP 200
status=UP
```

L2 财务报表端点无凭证访问:

```text
GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage?projectId=1001&bizCode=BX202606030968
HTTP 401
WWW-Authenticate: Bearer
```

`dts-copilot-analytics` 健康:

```text
GET http://127.0.0.1:50092/api/health
HTTP 200
status=UP
```

## 认证判断

- `dts-admin` 主安全链路为 OAuth2/JWT resource server，业务 API 不支持通用 `X-DTS-Service` 绕过。
- `APP_TEST_API_TOKEN` / `X-Test-Token` 只适用于 `/test/**` helper endpoints，不能作为 `/rs-flowers-base/...` 财务 L2 oracle 证据。
- 因 live L2 oracle 与 analytics `/api/dataset` 可能需要不同凭证，已按 TDD 增加分离配置:
  - `FINANCE_RECONCILIATION_ORACLE_AUTHORIZATION`
  - `FINANCE_RECONCILIATION_ORACLE_COOKIE`
  - `FINANCE_RECONCILIATION_ANALYTICS_AUTHORIZATION`
  - `FINANCE_RECONCILIATION_ANALYTICS_COOKIE`
  - 旧 `FINANCE_RECONCILIATION_AUTHORIZATION/COOKIE` 仍作为双边 fallback。

## 下一步所需

- 提供或生成具备财务 L2 报表访问权限的有效 JWT / 用户会话。
- 提供 analytics `/api/dataset` 的 API key 或有效认证。
- 配置两边认证后，执行 F1 live 双路取数并把差异输出纳入 IT-02。
