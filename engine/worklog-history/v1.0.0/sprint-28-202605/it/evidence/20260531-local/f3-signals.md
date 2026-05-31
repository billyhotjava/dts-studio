# F3 信号与跨域能力接入证据

## 范围

验证 F3-T01~T03 / IT02 / IT06:

- 锁定主动信号真实数据契约。
- 移除冷启动假信号。
- `/agent-bi?view=signals` 从真实 API 读取 ontology signals,失败时显示受控状态。
- 点击信号进入对话脊柱并携带 signal 上下文。

## RED

前端命令:

```bash
cd dts-copilot-webapp
pnpm test -- SignalsView coldStartCardsModel ColdStartHome AgentWorkspacePage
```

失败点:

- `SignalsView` 没有调用 `analyticsApi.listCopilotSignals("flowerbiz")`。
- `buildPlaceholderSignals(...)` 仍返回由资产/会话拼装的占位预警。
- 冷启动首屏仍可能展示“主动信号”业务卡。
- 点击信号后没有进入 `ConversationThread`。

后端命令:

```bash
mvn -pl dts-copilot-ai -Dtest=AiCopilotResourceTest test
```

失败点:

- `AiCopilotResource.SignalSummary` 不存在。
- `AiCopilotResource.signals(String domain)` 不存在。
- `AiCopilotResource` 未注入 `OntologyService`。

## GREEN

后端命令:

```bash
mvn -pl dts-copilot-ai -Dtest=AiCopilotResourceTest test
```

结果:

- `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- 断言 `flowerbiz` 返回当前语义包真实 signals: `坏账风险`、`欠费预警`。
- 断言未知 domain 返回空列表。

前端命令:

```bash
cd dts-copilot-webapp
pnpm test -- SignalsView coldStartCardsModel ColdStartHome AgentWorkspacePage
pnpm typecheck
```

结果:

- `pnpm test -- SignalsView coldStartCardsModel ColdStartHome AgentWorkspacePage`: 60 个 test files / 242 个 tests 全部通过。
- `pnpm typecheck`: exit 0。

覆盖行为:

- `SignalsView` 调用 `listCopilotSignals("flowerbiz")`。
- API 返回信号时展示标题、说明、风险等级和 linked action。
- 点击信号回调给工作台,工作台生成 `source=agent-workspace-signal` 且 `submit=true` 的 `CopilotPromptRequest`。
- `buildPlaceholderSignals(...)` 固定返回空数组。
- `ColdStartHome` 不展示“主动信号”假业务卡。

## Browser Smoke

环境:

```bash
cd dts-copilot-webapp
pnpm exec vite --host 127.0.0.1 --port 3005 --strictPort --open=false
```

说明:

- 使用本地 Vite dev server 和浏览器真实渲染。
- 本轮没有启动后端服务,所以 `/api/ai/copilot/signals?domain=flowerbiz` 的 Vite proxy `ECONNREFUSED` 是预期结果。
- smoke 验证的是前端运行时不会回落冷启动、不会展示假信号,并能显示受控错误。

`/agent-bi` 冷启动:

```json
{
  "url": "http://127.0.0.1:3005/agent-bi",
  "coldStart": true,
  "fakeSignalHeading": false,
  "fakeAssetSignal": false,
  "fakeResumeSignal": false,
  "signalCardText": [
    "继续上次会话暂无可继续会话。",
    "我的资产0 个看板 / 0 张查询打开资产库"
  ]
}
```

`/agent-bi?view=signals`:

```json
{
  "url": "http://127.0.0.1:3005/agent-bi?view=signals",
  "heading": "主动信号",
  "coldStart": false,
  "status": "信号数据读取失败，请稍后重试。",
  "empty": null,
  "fakeAssetSignal": false,
  "fakeResumeSignal": false,
  "buttons": []
}
```

## 结论

F3 已完成:

- 主动信号入口不再是空壳或冷启动死链。
- 前端消费 `dts-copilot-ai` ontology signals,不消费 `/api/alert` 资产告警。
- 冷启动首屏不再展示会被误解为真实业务预警的占位信号。
- linked action 已作为 signal prompt 上下文进入对话脊柱;实际审批面板继续由对话返回的 pending action 驱动。
