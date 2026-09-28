# F8 Live Contract / IT03 IT05 IT06 IT09

## Scope

- 验证环境: `docker compose` live 容器,入口为 `http://localhost:50080` / `50092` / `50091`。
- 本证据只记录已脱敏命令与关键返回字段;`X-Admin-Secret` 与 `X-Metabase-Session` 均不落文档。

## Pre-fix live gap

运行中 AI 容器只应用到 `v1_0_0_022`,DB 中 `v1_0_0_023__chat_message_copilot_contract.xml` 计数为 0。

```text
select count(*) ... v1_0_0_023__chat_message_copilot_contract.xml;
0
```

同一请求在重建前经过 AI internal / analytics 后均未返回 F8 字段:

```json
{
  "hasAssumptions": false,
  "hasTrace": false,
  "hasClarifications": false
}
```

重建前 SSE `done` 只有旧字段:

```json
{"templateCode":"PRS-FLOWERBIZ-OVERVIEW","responseKind":"FIXED_REPORT","routedDomain":"flowerbiz","targetView":"screen.prs-flowerbiz-overview-v1"}
```

## Runtime refresh

```text
mvn -pl dts-copilot-ai,dts-copilot-analytics -DskipTests package
BUILD SUCCESS

jar tf dts-copilot-ai/target/dts-copilot-ai-1.0.0-SNAPSHOT.jar
BOOT-INF/classes/com/yuzhi/dts/copilot/ai/service/copilot/CopilotChatContract.class
BOOT-INF/classes/com/yuzhi/dts/copilot/ai/service/copilot/CopilotChatRequestContext.class
BOOT-INF/classes/config/liquibase/changelog/v1_0_0_023__chat_message_copilot_contract.xml

docker compose build copilot-ai copilot-analytics copilot-webapp
Image dts-copilot-ai:latest Built
Image dts-copilot-analytics:latest Built
Image dts-copilot-webapp:latest Built

docker compose up -d copilot-ai copilot-analytics copilot-webapp
dts-copilot-ai Healthy
dts-copilot-analytics Healthy
dts-copilot-webapp Started
```

Post-refresh health and migration:

```text
curl http://localhost:50091/actuator/health -> {"status":"UP"}
curl http://localhost:50092/actuator/health -> {"status":"UP"}
curl -I http://localhost:50080/ -> 200 OK, Last-Modified: Sat, 30 May 2026 22:37:02 GMT

AI log:
Running Changeset: config/liquibase/changelog/v1_0_0_023__chat_message_copilot_contract.xml
... ran successfully in 10ms

select count(*) ... v1_0_0_023__chat_message_copilot_contract.xml;
1
```

## IT03 / IT05 live SSE

入口: `POST http://localhost:50080/api/copilot/chat/send-stream`

请求体:

```json
{
  "userMessage": "打开PRS租赁经营总览大屏",
  "datasourceId": null,
  "assumptionOverrides": {"period": "2026-05"},
  "clarificationAnswers": {"target": "在租项目"}
}
```

关键返回:

```text
event: session
event: token
event: done
```

```json
{
  "templateCode": "PRS-FLOWERBIZ-OVERVIEW",
  "responseKind": "FIXED_REPORT",
  "routedDomain": "flowerbiz",
  "targetView": "screen.prs-flowerbiz-overview-v1",
  "assumptions": [
    {"key": "domain", "value": "flowerbiz", "editable": false, "sourceHint": "planner"},
    {"key": "period", "value": "2026-05", "editable": true, "sourceHint": "user_override"},
    {"key": "clarification.target", "value": "在租项目", "editable": true, "sourceHint": "user_clarification"}
  ],
  "confidence": 0.64,
  "trace": {
    "metricCaliber": {"name": "screen.prs-flowerbiz-overview-v1", "domain": "flowerbiz"},
    "sources": [{"table": "screen.prs-flowerbiz-overview-v1", "role": "primary"}]
  }
}
```

结论:

- IT03: 文字提问经过 webapp nginx -> analytics -> AI,返回 SSE `session/token/done`。
- IT05: `assumptionOverrides.period` 被 live contract 回显为可编辑口径芯片,`sourceHint=user_override`。

## IT06 live clarification

入口: `POST http://localhost:50092/api/copilot/chat/send-stream`

低置信请求:

```json
{"userMessage":"执行","datasourceId":null}
```

关键返回:

```json
{
  "responseKind": "BUSINESS_CLARIFICATION",
  "routedDomain": "task",
  "targetView": "v_task_progress",
  "assumptions": [
    {"key": "domain", "value": "task", "editable": false, "sourceHint": "planner"}
  ],
  "confidence": 0.42,
  "clarifications": [
    {
      "key": "target",
      "question": "请确认本次要分析的业务对象。",
      "options": [{"value": "v_pendulum_progress", "label": "v_pendulum_progress"}]
    }
  ],
  "trace": {"metricCaliber": {"name": "v_task_progress", "domain": "task"}}
}
```

带澄清答案继续:

```json
{"userMessage":"执行","datasourceId":null,"clarificationAnswers":{"target":"v_pendulum_progress"}}
```

关键返回:

```json
{
  "generatedSql": "SELECT * FROM v_pendulum_progress ... LIMIT 100;",
  "responseKind": "BUSINESS_CLARIFICATION",
  "assumptions": [
    {"key": "domain", "value": "task", "sourceHint": "planner"},
    {"key": "clarification.target", "value": "v_pendulum_progress", "editable": true, "sourceHint": "user_clarification"}
  ],
  "confidence": 0.42,
  "trace": {
    "metricCaliber": {"name": "v_task_progress", "domain": "task"},
    "sql": "SELECT * FROM v_pendulum_progress ... LIMIT 100;"
  }
}
```

结论:低置信问题返回 `clarifications[]`;提交 `clarificationAnswers` 后不再返回 `clarifications[]`,并把用户选择作为可编辑 assumption 进入契约。

## IT09 live trace

入口: `POST http://localhost:50092/api/copilot/chat/send-stream`

请求体:

```json
{"userMessage":"查询2025年2月，绿萝这个产品的采购详细情况，按采购人、采购金额统计","datasourceId":null}
```

关键返回:

```json
{
  "generatedSql": "SELECT b.purchase_user_name, COUNT(*) AS row_count, ... ORDER BY purchase_amount DESC",
  "templateCode": "TPL-33",
  "responseKind": "TEMPLATE_SQL",
  "routedDomain": "procurement",
  "targetView": "authority.procurement.purchase_amount_by_buyer",
  "assumptions": [
    {"key": "domain", "value": "procurement", "editable": false, "sourceHint": "planner"}
  ],
  "confidence": 0.64,
  "trace": {
    "metricCaliber": {
      "name": "authority.procurement.purchase_amount_by_buyer",
      "domain": "procurement"
    },
    "sources": [
      {"table": "authority.procurement.purchase_amount_by_buyer", "role": "primary"}
    ],
    "sql": "SELECT b.purchase_user_name, COUNT(*) AS row_count, ... ORDER BY purchase_amount DESC"
  }
}
```

结论:`trace.metricCaliber` / `trace.sources[]` / `trace.sql` 在 live SSE `done` 中同时存在,可供前端 SQL 溯源面板直接消费。

## Cross-Evidence Notes

- IT04 语音提问 live 证据见 `f3-t04-composer.md`:headless Playwright 注入 `SpeechRecognition` final transcript,不 mock Copilot API;物理麦克风采集不在自动化范围内。
- IT07/IT08 资产沉淀 live 持久化证据见 `f7-asset-actions.md`:live 容器中完成 `POST /api/card`、`POST /api/dashboard/save` 和列表/详情可见性校验。
