# Sprint-27 集成测试(IT)证据

实施完成后,此处必须留下**真实验证证据**(命令输出、截图、用例结果),不得为空占位。

## 证据分层

- **Mock Contract**: 使用 mock SSE / mock API fixture 验证前端可消费 F8 新契约。
- **Degraded Runtime**: 真实后台尚未返回新字段时,验证 UI 降级不报错。
- **Live Contract**: 真实后台返回 F8 契约字段后,验证端到端完整闭环。

## IT 清单

| ID | 集成检查 | 关联 Feature | 阶段 | 状态 | 证据 |
|----|----------|--------------|------|------|------|
| IT00 | F3-T00/T01 测试护栏建立,T02 拆分结构 guard 通过,T05 会话状态 hook 通过 | F3 | P1a | DONE | `evidence/20260530-local/f3-t00-baseline.md`; `evidence/20260531-local/f3-t01-t02-conversation-split.md`; `evidence/20260531-local/f3-t05-session-state.md` |
| IT01 | 进入应用首屏为 agent 工作台冷启动态 | F1,F2 | P1a | DONE | `evidence/20260531-local/f2-cold-start-home.md` |
| IT02 | 旧路由已删 / `/public/*` 仍可访问(逐路由验证) | F1 | P1a | DONE | `evidence/20260531-local/f1-shell-navigation-public.md` |
| IT03 | 文字提问 → SSE 流式 → 出结果全链路 | F3,F5 | P1a | DONE | Mock/Hook: `evidence/20260531-local/f3-t03-stream-hook.md`; F5 contract: `evidence/20260531-local/f5-t02-optimistic-contract.md`; Live Contract: `evidence/20260531-local/f8-live-contract.md` |
| IT04 | 语音提问 → 出结果 | F2,F3,F5 | P1a | DONE | Component/Mock + Live: `evidence/20260531-local/f3-t04-composer.md` |
| IT05 | 口径芯片可改 → 带 `assumptionOverrides` 重算 → 同 id 画布刷新 | F5,F8 | P1b | DONE | Mock Contract: `evidence/20260531-local/f5-t04-recompute-artifact.md`; Backend Contract: `evidence/20260531-local/f8-backend-contract.md`; Live Contract: `evidence/20260531-local/f8-live-contract.md` |
| IT06 | 低置信问题降级为反问 → 带 `clarificationAnswers` 继续执行 | F5,F8 | P1b | DONE | Mock Contract: `evidence/20260531-local/f5-t03-clarification-chips.md`; Backend Contract: `evidence/20260531-local/f8-backend-contract.md`; Live Contract: `evidence/20260531-local/f8-live-contract.md` |
| IT07 | 结果「存为卡片」→ 资产库可见 | F7 | P1c | DONE | Mock/Contract + Live: `evidence/20260531-local/f7-asset-actions.md` |
| IT08 | 结果「钉到看板」→ 看板可见 | F7 | P1c | DONE | Mock/Contract + Live: `evidence/20260531-local/f7-asset-actions.md` |
| IT09 | 溯源面板展示口径/表/SQL | F6,F8 | P1b | DONE | Mock/Degraded: `evidence/20260531-local/f6-trace-panel.md`; Backend Contract: `evidence/20260531-local/f8-backend-contract.md`; Live Contract: `evidence/20260531-local/f8-live-contract.md` |
| IT10 | `pnpm typecheck` + `pnpm test` + `pnpm build` 全绿 | 全部 | 全部 | DONE | `evidence/20260531-local/f6-trace-panel.md`; `evidence/20260531-local/f7-asset-actions.md`; `evidence/20260531-local/f8-backend-contract.md` |

## 验证环境

- 前端:`dts-copilot-webapp`,`VITE_CACHE_DIR=.vite-cache pnpm exec vite --host 0.0.0.0 --port 3004 --strictPort --open=false`(本机 3003 已被占用)
- 验证方式:Playwright E2E + 手工截图存 `../assets/`
- F8 contract 层已完成并记录 Backend Contract 证据;`evidence/20260531-local/f8-live-contract.md` 已通过 live 容器验证 webapp nginx → analytics → AI 的 SSE 契约字段。
- Sprint-27 IT00-IT10 均已有对应验证证据;语音 live 边界为 headless Playwright 注入 `SpeechRecognition` 转写事件后走真实 SSE。
