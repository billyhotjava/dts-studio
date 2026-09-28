# T01: 取消右侧 CanvasPanel 并恢复 inline 自动预览

**优先级**: P0
**状态**: DONE
**依赖**: F1,F3

## 目标

把 Agent BI 工作台从“左侧对话 + 右侧画布”的双结果面收敛为单窗口体验。

## 技术设计

- `AgentWorkspacePage` 移除右侧 `CanvasPanel` 和对应 `artifactStore`/资产弹窗接线。
- `AgentWorkspacePage.css` 将 active conversation 布局改为居中单列脊柱。
- `MessageList` 取消 `preferCanvasPreview` 开关,最新 SQL 回答始终由 inline preview 自动预览。
- 测试断言工作台不再挂载 canvas shell,并断言 `ConversationThread` 不再收到 `artifactStore`。

## 影响范围

- `dts-copilot-webapp/src/pages/AgentWorkspacePage.tsx`
- `dts-copilot-webapp/src/pages/AgentWorkspacePage.css`
- `dts-copilot-webapp/src/pages/AgentWorkspacePage.test.ts`
- `dts-copilot-webapp/src/components/copilot/MessageList.tsx`
- `dts-copilot-webapp/src/components/copilot/ConversationThread.tsx`
- `dts-copilot-webapp/src/components/copilot/MessageList.inlinePreview.test.tsx`

## 验证

- [x] `pnpm test -- src/pages/AgentWorkspacePage.test.ts src/components/copilot/MessageList.inlinePreview.test.tsx`
- [x] `pnpm typecheck`
- [x] `pnpm build`
- [x] `docker compose build copilot-webapp && docker compose up -d --force-recreate --no-deps copilot-webapp`
- [x] `docker exec dts-copilot-webapp sh -lc 'grep -R "agent-workspace__canvas\\|CanvasPanel" -n /usr/share/nginx/html/assets/AgentWorkspacePage-* 2>/dev/null || true'` 无输出

## 完成标准

- [x] `/agent-bi` 进入会话后只有对话窗口一个结果面。
- [x] 右侧预览栏不会再出现。
- [x] inline SQL 预览保持自动执行。
