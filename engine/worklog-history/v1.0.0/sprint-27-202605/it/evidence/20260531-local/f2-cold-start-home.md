# Sprint-27 F2 冷启动首屏验证

**日期**: 2026-05-31
**范围**: F2-T01 ~ F2-T04, IT01 / IT10
**模块**: `dts-copilot-webapp`

## 变更摘要

- 新增 `ColdStartHome`、`ColdStartComposer`、`StarterChips`、`ColdStartCards` 及模型函数,并接入 `/agent-bi` 空态。
- 大输入框支持回车发送、`Shift+Enter` 换行、空白拦截;提交后派发 `dts:ai-quick-ask` 供 F3 对话脊柱接管。
- 语音入口复用 `VoiceInputButton`,最终转写回填输入框,可直接发送。
- 起手芯片优先读取 `listSuggestedQuestions`,失败/为空回退现有 Welcome/Agent 报表问题体系;固定报表芯片直接发起报花月报查询。
- 三张卡接入看板/查询/会话数据,主动信号使用规则化占位;接口失败时独立降级,不影响输入框。
- 移动端冷启动首屏阻止 header/composer/chips/cards 互相 flex-shrink,小屏改为从顶部顺序布局并滚动,避免起手问题覆盖输入框。

## 验证命令

```bash
cd dts-copilot-webapp
pnpm exec vitest run src/components/copilot/cold-start
```

结果: 4 个测试文件通过,14 个测试通过。

```bash
cd dts-copilot-webapp
pnpm typecheck
```

结果: 通过。

```bash
cd dts-copilot-webapp
pnpm test
```

结果: 33 个测试文件通过,140 个测试通过。

```bash
cd dts-copilot-webapp
pnpm build
```

结果: 通过。`validate:screen-plugins` 与 `validate:screen-plugin-boundary` 均通过;Vite 仍提示既有大 chunk warning,不影响构建结果。

## Follow-up Smoke

```bash
cd dts-copilot-webapp
pnpm exec vitest run src/components/copilot/cold-start/ColdStartHome.test.tsx src/layouts/AppLayout.workspace.test.ts
# Test Files 2 passed; Tests 12 passed
```

- Playwright mobile smoke(390x844):输入框高度保持 167px,起手问题从 `y=384` 开始,不再覆盖输入框;后续卡片通过页面滚动继续展示。

## 备注

- F2 只负责冷启动发起;真正 SSE 流式结果进入 F3/F5 验收。
- 浏览器真实语音权限/麦克风链路仍归 IT04 后续端到端验证;本次覆盖 `VoiceInputButton` 接入与最终转写回填行为。
