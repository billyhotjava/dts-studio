# Sprint-28 Signals Contract

## 结论

F3 采用 `dts-copilot-ai` 的 ontology signals 作为主动信号唯一数据源:

```text
GET /api/ai/copilot/signals?domain=flowerbiz
```

接口返回 `ApiResponse<List<SignalSummary>>`,前端通过 `analyticsApi.listCopilotSignals("flowerbiz")` 消费。`dts-copilot-analytics` 的 `/api/alert` 是卡片/看板告警配置面,不是 Sprint-26 Tier2 ontology signal 数据源,本次不接入。

## 后端链路

入口:

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiCopilotResource.java`

数据来源:

- `OntologyService.load(domain)`
- `OntologyModel.buildSignalPlans()`
- `dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json`

当前 `flowerbiz` 语义包包含:

| title | severity | objectName | linkedActions |
|-------|----------|------------|---------------|
| 坏账风险 | high | 坏账汇总 | 创建坏账处理单 |
| 欠费预警 | medium | 客户月度汇总 | - |

## DTO

```json
{
  "id": "flowerbiz:坏账风险",
  "title": "坏账风险",
  "severity": "high",
  "description": "项目坏账率超过 15% 且存在租金损失，建议核对坏账明细并发起坏账处理单草稿。",
  "source": "ontology.flowerbiz.signals",
  "objectName": "坏账汇总",
  "linkedActions": ["创建坏账处理单"]
}
```

字段说明:

| 字段 | 来源 | 用途 |
|------|------|------|
| `id` | `{domain}:{signalName}` | 前端 key、对话 `reportIntentId` |
| `title` | `SignalPlan.signalName` | 信号标题 |
| `severity` | `SignalPlan.severity` | 前端风险等级展示 |
| `description` | `SignalPlan.advice` | 用户可读说明 |
| `source` | `ontology.{domain}.signals` | 来源追溯 |
| `objectName` | `SignalPlan.objectName` | 命中对象范围 |
| `linkedActions` | `SignalPlan.linkedActions` | 后续审批/写回动作提示 |

## 前端策略

- `/agent-bi?view=signals` 进入 `SignalsView`,不回落冷启动。
- `SignalsView` 只展示接口返回的信号;接口失败时显示“信号数据读取失败，请稍后重试。”。
- 接口返回空列表时显示“信号数据未接通”空态。
- 冷启动首屏不再展示 `buildPlaceholderSignals()` 拼出来的业务预警;该函数保留兼容但固定返回空数组。
- 点击信号进入 `ConversationThread`,自动提交包含信号标题、说明、来源和 linked action 的 prompt。

## 验证

- `mvn -pl dts-copilot-ai -Dtest=AiCopilotResourceTest test`
- `pnpm test -- SignalsView coldStartCardsModel ColdStartHome AgentWorkspacePage`
- `pnpm typecheck`
- Playwright smoke 见 `it/evidence/20260531-local/f3-signals.md`
