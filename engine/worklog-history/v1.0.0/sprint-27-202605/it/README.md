# Sprint-27 集成测试(IT)证据

实施完成后,此处必须留下**真实验证证据**(命令输出、截图、用例结果),不得为空占位。

## IT 清单

| ID | 集成检查 | 关联 Feature | 状态 | 证据 |
|----|----------|--------------|------|------|
| IT01 | 进入应用首屏为 agent 工作台冷启动态 | F1,F2 | TODO | - |
| IT02 | 旧路由已删 / `/public/*` 仍可访问(逐路由验证) | F1 | TODO | - |
| IT03 | 文字提问 → SSE 流式 → 出结果全链路 | F3,F5 | TODO | - |
| IT04 | 语音提问 → 出结果 | F2,F3,F5 | TODO | - |
| IT05 | 口径芯片可改 → 结果重算 | F5 | TODO | - |
| IT06 | 低置信问题降级为反问 | F5 | TODO | - |
| IT07 | 结果「存为卡片」→ 资产库可见 | F7 | TODO | - |
| IT08 | 结果「钉到看板」→ 看板可见 | F7 | TODO | - |
| IT09 | 溯源面板展示口径/表/SQL | F6 | TODO | - |
| IT10 | `pnpm typecheck` + `pnpm test` + `pnpm build` 全绿 | 全部 | TODO | - |

## 验证环境

- 前端:`dts-copilot-webapp`,`pnpm dev`(端口 3003)
- 验证方式:Playwright E2E + 手工截图存 `../assets/`
