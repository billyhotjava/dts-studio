# IT09 最终验证

## 命令验证

```bash
cd dts-copilot-webapp
pnpm typecheck
pnpm test
pnpm build
node --test tests/appShellConfig.test.ts

cd ..
mvn -pl dts-copilot-ai -Dtest=AiCopilotResourceTest test
```

结果:

- `pnpm typecheck`: exit 0。
- `pnpm test`: 60 个 test files / 244 个 tests 全部通过。
- `pnpm build`: exit 0;保留既有大 chunk warning。
- `node --test tests/appShellConfig.test.ts`: 2/2 通过。
- `mvn -pl dts-copilot-ai -Dtest=AiCopilotResourceTest test`: 2/2 通过。

## Browser Smoke

环境:

```bash
cd dts-copilot-webapp
pnpm exec vite --host 127.0.0.1 --port 3005 --strictPort --open=false
```

说明:

- 本轮没有启动后端服务,所以 API proxy `ECONNREFUSED` 是预期结果。
- smoke 验证前端路由、query 消费、redirect 和受控错误态。

`/agent-bi?view=sessions`:

```json
{
  "url": "http://127.0.0.1:3005/agent-bi?view=sessions",
  "heading": "历史会话",
  "coldStart": false,
  "sessionsView": true,
  "status": "历史会话读取失败，请稍后重试。",
  "loginVisible": false
}
```

`/agent-bi?view=signals`:

```json
{
  "url": "http://127.0.0.1:3005/agent-bi?view=signals",
  "heading": "主动信号",
  "coldStart": false,
  "signalsView": true,
  "status": "信号数据读取失败，请稍后重试。",
  "fakeAssetSignal": false,
  "fakeResumeSignal": false
}
```

`/agent-bi?fixedReport=PRS-FLOWERBIZ-OVERVIEW`:

```json
{
  "url": "http://127.0.0.1:3005/agent-bi?fixedReport=PRS-FLOWERBIZ-OVERVIEW",
  "heading": "未找到固定报表模板：PRS-FLOWERBIZ-OVERVIEW",
  "coldStart": false,
  "fixedReportStatus": true,
  "conversation": false
}
```

资产库 tabs:

```json
[
  {
    "url": "http://127.0.0.1:3005/assets?tab=dashboards",
    "headings": ["资产库"],
    "activeTab": "看板"
  },
  {
    "url": "http://127.0.0.1:3005/assets?tab=cards",
    "headings": ["资产库"],
    "activeTab": "卡片"
  },
  {
    "url": "http://127.0.0.1:3005/assets?tab=collections",
    "headings": ["资产库"],
    "activeTab": "集合"
  }
]
```

旧列表入口 redirect:

```json
[
  {
    "from": "/dashboards",
    "url": "http://127.0.0.1:3005/assets?tab=dashboards",
    "headings": ["资产库"],
    "activeTab": "看板"
  },
  {
    "from": "/questions",
    "url": "http://127.0.0.1:3005/assets?tab=cards",
    "headings": ["资产库"],
    "activeTab": "卡片"
  },
  {
    "from": "/collections",
    "url": "http://127.0.0.1:3005/assets?tab=collections",
    "headings": ["资产库"],
    "activeTab": "集合"
  }
]
```

## 结论

Sprint-28 的 P0/P1/P2 收口项均已完成。当前没有发现 F1~F5 相关的明显运行时死链、占位信号误导、双入口布局或状态簿记自相矛盾问题。
