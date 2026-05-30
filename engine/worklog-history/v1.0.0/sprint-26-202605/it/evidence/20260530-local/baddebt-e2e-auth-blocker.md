# Sprint-26 F3/T04 一键坏账草稿端到端联调阻塞记录

**时间**: 2026-05-30
**范围**: 真实 adminapi `saveDraftFlowerBadDebt` 草稿创建链路。

## 运行态探测

容器状态显示 `v223-dts-admin-1` 已运行，并暴露本机端口 `38012->8081`。
当前本机未运行旧 PRS `rs-gateway` / `rs-flowers-base` 容器；`docker ps -a` 只看到 `v223-dts-admin-1`，没有 `rs-*` / `flowerbase` 运行实例。

命令：

```bash
curl -sS -o /tmp/baddebt_probe.out -w '%{http_code}' \
  -X POST http://127.0.0.1:38012/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt \
  -H 'Content-Type: application/json' \
  --data '{"projectId":101,"draftItemJson":"[501]","badDebtType":1}'
```

结果：

```text
401
```

2026-05-30 重试补充：

```text
无 Authorization 直连 38012 /rs-flowers-base/... => 401
client_credentials 服务账号 token 直连 38012 /rs-flowers-base/... => 401 会话已超时，请重新登录
token-exchange sysadmin + 临时 admin_sessions 记录直连 38012 /rs-flowers-base/... => 403
http://127.0.0.1:50081/rs-flowers-base/... => 308 到 HTTPS
https://127.0.0.1:50443/rs-flowers-base/... => 405 nginx webapp
```

补充定位：

- `38012` 是 v2.2.3 `dts-admin`，不是 PRS 旧 `rs-gateway`。该服务 `SecurityConfiguration` 只显式放行/授权 `/api/**`、`/admin/**`、`/management/**` 等路径；`/rs-flowers-base/**` 带有效三员 token 后会落到未匹配路径并返回 `403`。
- `dts-copilot-proxy` 只路由 `/api/ai`、`/api` 和 `/` 到 copilot 自身服务，裸 `/rs-flowers-base/**` 会落到 webapp，POST 返回 `405`。
- `dts-copilot-ai` 当前运行容器未配置 `COPILOT_ACTION_ADMINAPI_BASE_URL` / `COPILOT_ACTION_ADMINAPI_AUTHORIZATION`。

## 结论

- 之前的 `401` 不是唯一问题：当前配置/手工探测使用的 base URL 指向了错误服务，尚未连到 PRS 旧 adminapi gateway。
- 端到端仍需要两个运行态前提：正确的 PRS adminapi base URL，以及该 adminapi 接受的业务 Authorization。
- 在未提供正确 base URL + 业务 token 前，无法完成“真实创建草稿 → adminweb 可见待确认 → 审计链路”端到端验收。

## 已补验证

命令：

```bash
mvn -pl dts-copilot-ai -Dtest=HttpAdminApiActionClientTest test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

覆盖点：

- `HttpAdminApiActionClient` 在配置 `copilot.action.adminapi.authorization` 后会向 draft endpoint 发送 `Authorization`。
- 未配置时维持原有无授权头行为。
- `HttpAdminApiActionClient` 在缺少 `copilot.action.adminapi.base-url` 时直接返回配置错误，避免默认误打 `dts-copilot-proxy`。
