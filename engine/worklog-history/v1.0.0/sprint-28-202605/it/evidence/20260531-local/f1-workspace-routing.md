# F1 工作台路由接线证据

## 范围

本文件记录 F1-T01~T04 / IT01~IT03: `/agent-bi?view=sessions`、`/agent-bi?view=signals`、`/agent-bi?fixedReport=...` 三类工作台 query 的接线和浏览器 smoke。

## RED

命令:

```bash
cd dts-copilot-webapp
pnpm test -- AgentWorkspacePage
```

结果:

- 新增 `view=sessions` 测试后失败。
- 失败表现:页面仍渲染 `cold-start-home`,找不到 heading `历史会话`,也找不到历史会话按钮。
- 新增 `view=signals` 测试后失败。
- 失败表现:`normalizeAgentWorkspaceView("signals")` 返回 `home`,页面仍回落冷启动。
- 新增 `fixedReport` / `fixedReportTemplate` 测试后失败。
- 失败表现:找不到 `prompt-request`,空 `fixedReport=` 仍回落冷启动,没有明确错误态。

## GREEN

命令:

```bash
cd dts-copilot-webapp
pnpm test -- AgentWorkspacePage
pnpm typecheck
pnpm build
```

T01 结果:

- `pnpm test -- AgentWorkspacePage`: 59 个 test files / 232 个 tests 全部通过。
- `pnpm typecheck`: exit 0。
- `pnpm build`: exit 0;保留既有 `vendor-echarts` 等大 chunk warning。

T02/T03 追加结果:

- `pnpm test -- AgentWorkspacePage`: 59 个 test files / 237 个 tests 全部通过。
- `pnpm typecheck`: exit 0。
- 新增覆盖:
  - `view=signals` 规范化与受控空态。
  - `fixedReport` 查询目录成功后生成自动提交 `CopilotPromptRequest`。
  - `fixedReportTemplate` 查询目录成功后生成自动提交 `CopilotPromptRequest`。
  - 空 `fixedReport=` 展示“固定报表模板参数为空”,不渲染冷启动或对话脊柱。

## Browser Smoke

环境:

```bash
cd dts-copilot-webapp
PORT=3005 VITE_CACHE_DIR=.vite-cache pnpm exec vite --host 0.0.0.0 --port 3005 --strictPort --open=false
```

Playwright 打开:

```text
http://localhost:3005/agent-bi?view=sessions
```

说明:

- 为验证当前前端源码,使用 Vite dev server。
- sessions API、session detail API、database API、analysis-drafts API 在 Playwright 中用 route mock 成稳定数据。
- 该 smoke 证明前端路由消费和视图渲染,不冒充后端 live 证据。

断言结果:

```json
{
  "url": "http://localhost:3005/agent-bi?view=sessions",
  "titleVisible": true,
  "sessionVisible": true,
  "coldStartVisible": false,
  "conversationVisible": true,
  "restoredMessageVisible": true
}
```

追加 Playwright smoke:

环境:

```bash
cd dts-copilot-webapp
pnpm exec vite --host 127.0.0.1 --port 3005 --strictPort
```

说明:

- 使用本地 Vite dev server 和浏览器真实渲染。
- 通过 `platformUserStore` 写入本地 smoke token 绕过 standalone 登录检查。
- 本轮没有启动后端服务,所以 sessions 和 fixedReport 的 API 失败态是预期结果;验证重点是 query 被消费、页面不回落冷启动、错误态可见。

`/agent-bi?view=sessions`:

```json
{
  "title": "历史会话",
  "coldStart": false,
  "sessionsView": true,
  "status": "历史会话读取失败，请稍后重试。",
  "loginVisible": false
}
```

`/agent-bi?view=signals`:

```json
{
  "title": "主动信号",
  "emptyTitle": "信号数据未接通",
  "coldStart": false,
  "signalsView": true,
  "fakeAssetSignal": false,
  "fakeResumeSignal": false,
  "loginVisible": false
}
```

`/agent-bi?fixedReport=PRS-FLOWERBIZ-OVERVIEW`:

```json
{
  "title": "未找到固定报表模板：PRS-FLOWERBIZ-OVERVIEW",
  "coldStart": false,
  "fixedReportStatus": true,
  "conversation": false,
  "loginVisible": false
}
```

## 结论

F1 已完成:

- `/agent-bi?view=sessions` 会渲染历史会话视图。
- 历史会话视图读取 `analyticsApi.listAiAgentSessions(50)`。
- 空态、加载态、错误态存在。
- 点击会话会将 `focusRequest` 交给 `ConversationThread`,进入对话脊柱。
- `/agent-bi?view=signals` 会渲染受控信号视图;真实 signals 未接通前不展示占位业务预警。
- `/agent-bi?fixedReport=...` 会保留模板上下文并查询固定报表目录;目录可用时生成自动提交的 `CopilotPromptRequest`,目录不可用时展示明确错误态。
