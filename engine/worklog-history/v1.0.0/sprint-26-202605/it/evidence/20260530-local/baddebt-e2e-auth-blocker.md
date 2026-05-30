# Sprint-26 F3/T04 一键坏账草稿端到端联调记录

**时间**: 2026-05-30
**范围**: 真实 PRS adminapi `saveDraftFlowerBadDebt` 草稿创建链路。

## 原阻塞

最初手工探测误用了 v2.2.3 / copilot 运行态入口：

```text
http://127.0.0.1:38012/rs-flowers-base/... => 401/403
http://127.0.0.1:50081/rs-flowers-base/... => 308 到 HTTPS
https://127.0.0.1:50443/rs-flowers-base/... => 405 nginx webapp
```

定位结论：

- `38012` 是 v2.2.3 `dts-admin`，不是 PRS 旧 `rs-gateway`；`/rs-flowers-base/**` 带有效三员 token 后仍会被 dts-admin 安全配置拒绝。
- `dts-copilot-proxy` 只路由 copilot 自身 `/api/ai`、`/api` 和 `/`，裸 `/rs-flowers-base/**` 会落到 webapp。
- `dts-copilot-ai` 运行容器当时未配置 `COPILOT_ACTION_ADMINAPI_BASE_URL` / `COPILOT_ACTION_ADMINAPI_AUTHORIZATION`。

## 本次重试修复

已在本地临时 old PRS 栈完成以下运行态修复：

- 将旧 adminapi Docker base image 从失效的 `openjdk:8-jre` 切到 `eclipse-temurin:8-jre-focal`。
- 修复 flowerbase Dockerfile 的 Ubuntu security mirror 403。
- 避开 v2.2.3 Docker 网段冲突：old PRS compose 改为 `172.23.0.0/16`。
- 启动临时本地 MySQL `prs-old-mysql`，导入 old PRS seed 库，并将 Nacos JDBC 指向本地 MySQL。
- 修复 Nacos shared config 的 Redis host，gateway `/code` 已能生成验证码。
- 修复 old PRS runtime 两处启动问题：`LogAspect` stale target class、flowerbase `JacksonConfig` bean name 冲突。
- 本地临时库补齐 `t_flower_biz_info` 与当前实体不一致的缺失列，避免草稿保存 SQL 失败。
- 本地临时 admin 用户密码已重置为测试密码，仅用于本地联调；未记录 token 或真实密钥。

## 验证结果

old PRS 最小栈已启动：

```text
docker-nacos-1            Up healthy
docker-redis-1            Up
docker-gateway-1          Up, 7091/tcp
docker-auth-1             Up, 7092/tcp
docker-modules-system-1   Up, 7093/tcp
docker-flowerbase-1       Up, 7095/tcp
```

未登录探测坏账列表：

```text
GET /rs-flowers-base/flower/bizBadDebt/listPage?pageNum=1&pageSize=1
=> {"code":401,"msg":"登录状态已过期,重新登录"}
```

登录后探测坏账列表：

```text
/code => 200，验证码写入 Redis
/auth/login => code=200，has_token=true
GET /rs-flowers-base/flower/bizBadDebt/listPage?pageNum=1&pageSize=1
=> {"code":200,"msg":"查询成功","rows":[],"total":0}
```

真实草稿创建：

```text
POST /rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt
Authorization: Bearer <redacted>

payload:
{
  "remark": "copilot-t04-20260530224032",
  "urgent": 2,
  "badDebtType": 1,
  "draftItemJson": "[{\"source\":\"copilot-e2e\",\"reason\":\"bad debt draft route verification\",\"marker\":\"copilot-t04-20260530224032\"}]"
}

response:
{"code":200,"msg":"操作成功","data":{"id":2060733103860617216,"status":20,"bizType":6,"remark":"copilot-t04-20260530224032"}}

DB count:
biz_type=6 and status=20 before=0 after=1
```

## 结论

- 原运行态入口阻塞已解除：正确 PRS adminapi gateway + 业务 Authorization 已可用。
- `saveDraftFlowerBadDebt` 已在真实 old PRS adminapi 上创建草稿，返回 `status=20`、`bizType=6`。
- 正式提交端点 `saveFlowerBadDebt` 未调用，符合 copilot 只创建草稿、不代提交的边界。

## 补充覆盖

- 已用正在运行的 `dts-copilot-ai` 容器配置真实 `COPILOT_ACTION_ADMINAPI_BASE_URL` / 短期业务 Authorization 后跑完整“聊天 approve -> adminapi draft -> 审计日志”运行态闭环，详见 `action-runtime-live.md`。
- adminweb 页面未单独截图；本次以 adminweb 坏账列表页同源 `listPage` 接口返回作为可见性证据，详见 `action-runtime-live.md`。

## 已补自动化验证

```bash
mvn -pl dts-copilot-ai -Dtest=HttpAdminApiActionClientTest test
RUN_TEST=1 bash worklog/v1.0.0/sprint-26-202605/it/test_action_chat_approval_api.sh
```

覆盖点：

- `HttpAdminApiActionClient` 配置 `copilot.action.adminapi.authorization` 后会向 draft endpoint 发送 `Authorization`。
- 缺少 `copilot.action.adminapi.base-url` 时 fail-fast，避免默认误打 `dts-copilot-proxy`。
- 聊天 `/approve` / `/cancel` API 已接到审批服务，审批表单参数会按 `projectId`、`draftItemJson`、`badDebtType` 透传到 action executor。
