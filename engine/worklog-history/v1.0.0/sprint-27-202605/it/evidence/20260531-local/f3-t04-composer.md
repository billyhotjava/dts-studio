# F3-T04 输入器与语音整合证据

**时间**: 2026-05-31
**范围**: `Composer.tsx` / `VoiceInputButton` 接入 / 输入行为

## 覆盖内容

- `Composer.test.tsx`
  - Enter 提交当前文本。
  - Shift+Enter 不提交,保留多行输入语义。
  - 输入法组合态 / `keyCode=229` 不误提交。
  - final 语音转写追加到已有文本并可提交。
  - streaming 时发送按钮进入“停止”动作,点击只触发 stop。
  - 不可编辑时 textarea 与语音按钮禁用。
- 继续复用既有纯函数测试:
  - `copilotInputBehavior.test.ts`
  - `copilotComposerState.test.ts`
  - `copilotStreamControl.test.ts`
  - `useVoiceInput.test.ts`

## 验证命令

- `pnpm vitest run src/components/copilot/Composer.test.tsx`
  - 1 file passed,5 tests passed。
- `pnpm test`
  - 36 files passed,157 tests passed。
- `pnpm typecheck`
  - 通过。

## 未覆盖

物理麦克风权限/真实音频采集不在 headless 环境自动化范围内;Live 验收使用 Playwright 注入标准 `SpeechRecognition` mock,只替代浏览器转写结果,不 mock Copilot API。

## Live Contract

入口:`http://localhost:50080/agent-bi`;认证使用本机有效 `metabase.SESSION` cookie,文档不记录 session 值。

Playwright 验证步骤:

1. 页面加载前注入 `window.SpeechRecognition` / `window.webkitSpeechRecognition` mock。
2. 点击冷启动首屏「语音输入」按钮。
3. mock 触发 final transcript:`打开PRS租赁经营总览大屏`。
4. 确认 textarea 已回填该 transcript。
5. 点击「发送」。
6. 等待真实 `POST /api/copilot/chat/send-stream` 返回 200。
7. 等待页面渲染 PRS 租赁经营总览相关结果与画布信号。

关键返回:

```json
{
  "voiceStarted": true,
  "transcript": "打开PRS租赁经营总览大屏",
  "streamRequestUrl": "http://localhost:50080/api/copilot/chat/send-stream",
  "streamResponseStatus": 200,
  "streamResponses": [
    {
      "url": "http://localhost:50080/api/copilot/chat/send-stream",
      "status": 200
    }
  ],
  "renderedSignals": {
    "hasPrsOverview": true,
    "hasCanvas": true
  }
}
```

结论:语音 final transcript 能进入同一提交链路,并通过 live webapp nginx → analytics → AI SSE 出结果;IT04 可标 Live DONE。
