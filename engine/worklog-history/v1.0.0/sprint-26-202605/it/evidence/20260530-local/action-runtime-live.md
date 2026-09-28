# Sprint-26 F3/T04 copilot-ai 运行态 Action 闭环证据

**时间**: 2026-05-30
**范围**: `dts-copilot-ai` 运行容器 -> PRS old adminapi -> Copilot 审计日志 -> adminweb 坏账列表数据源。

## 配置修复

- `dts-copilot/docker-compose.yml` 已为 `copilot-ai` 增加：
  - `COPILOT_ACTION_ADMINAPI_BASE_URL`
  - `COPILOT_ACTION_ADMINAPI_AUTHORIZATION`
- `HttpAdminApiActionClient` 继续通过 `copilot.action.adminapi.base-url` / `copilot.action.adminapi.authorization` 读取配置。
- `dts-copilot-ai` 使用短期业务 Authorization 重建运行容器；密钥不入库、不写证据。
- `dts-copilot-ai` 已接入 old PRS adminapi 所在 Docker 网络，容器内可访问 `http://docker-gateway-1:7091`。

## 可重跑脚本

```bash
bash worklog/v1.0.0/sprint-26-202605/it/test_action_runtime_env_wiring.sh

admin_secret=$(sed -n 's/^COPILOT_ADMIN_SECRET=//p' .env | tr -d '\r' | tail -1)
RUN_LIVE=1 \
  COPILOT_AI_BASE_URL='http://127.0.0.1:50091' \
  COPILOT_ADMIN_SECRET="$admin_secret" \
  bash worklog/v1.0.0/sprint-26-202605/it/test_action_runtime_env_wiring.sh
```

## 验证结果

静态 compose/env 渲染：

```text
[static] copilot-ai adminapi action env wiring is renderable
```

`copilot-ai` 健康检查：

```text
GET http://127.0.0.1:50091/actuator/health => 200
```

live approve：

```text
[static] copilot-ai adminapi action env wiring is renderable
[live] chat approve created adminapi draft through copilot-ai
```

## Copilot 审计日志

```text
326|sprint26-action-it|sess-copilot-runtime-t04-20260530225405|ACTION_EXECUTION|创建坏账处理单|t|flowerbiz:baddebt:draft|{"message":"操作成功","responseBody":{"msg":"操作成功","code":200,...
```

结论：

- `user_id=sprint26-action-it`
- `action=ACTION_EXECUTION`
- `tool_name=创建坏账处理单`
- `success=true`
- `guard=flowerbiz:baddebt:draft`

## PRS adminapi / DB 草稿

最新草稿行：

```text
02060736510340108288  status=20  biz_type=6  bad_debt_type=1  create_time=2026-05-30 22:54:05
draft_item_json contains marker copilot-runtime-t04-20260530225405
```

adminapi 坏账列表接口返回：

```text
GET /rs-flowers-base/flower/bizBadDebt/listPage?pageNum=1&pageSize=5
=> code=200, msg=查询成功, total=4

first row:
id=2060736510340108288
status=20
bizType=6
badDebtType=1
draftItemJson contains marker copilot-runtime-t04-20260530225405
```

## adminweb 可见性说明

`adminweb/src/api/flower/flowerbiz/flowerBadDebt.js` 的坏账列表方法调用同一个 endpoint：

```text
/rs-flowers-base/flower/bizBadDebt/listPage
```

`adminweb/src/views/flower/flowerbiz/baddebt/list-baddebt-flower.vue` 使用该 `listPage` 加载坏账列表。上述接口已返回最新草稿，因此 adminweb 的数据源层面已可见该待处理草稿。本次未单独构建 old PRS adminweb 截图。

## 安全边界

- Copilot 只调用 `saveDraftFlowerBadDebt` 创建草稿。
- 未调用 `saveFlowerBadDebt` 正式提交端点。
- 未直写 PRS 业务库；DB 查询仅用于验收核对。
