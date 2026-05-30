# Sprint-27 集成测试(IT)证据

实施完成后,此处必须留下**真实验证证据**(命令输出、截图、用例结果),不得为空占位。

## 证据分层

- **Mock Contract**: 使用 mock SSE / mock API fixture 验证前端可消费 F8 新契约。
- **Degraded Runtime**: 真实后台尚未返回新字段时,验证 UI 降级不报错。
- **Live Contract**: 真实后台返回 F8 契约字段后,验证端到端完整闭环。

## IT 清单

| ID | 集成检查 | 关联 Feature | 阶段 | 状态 | 证据 |
|----|----------|--------------|------|------|------|
| IT00 | F3-T00 现状测试基线建立,删除旧路由前后均通过 | F3 | P1a | TODO | - |
| IT01 | 进入应用首屏为 agent 工作台冷启动态 | F1,F2 | P1a | TODO | - |
| IT02 | 旧路由已删 / `/public/*` 仍可访问(逐路由验证) | F1 | P1a | TODO | - |
| IT03 | 文字提问 → SSE 流式 → 出结果全链路 | F3,F5 | P1a | TODO | - |
| IT04 | 语音提问 → 出结果 | F2,F3,F5 | P1a | TODO | - |
| IT05 | 口径芯片可改 → 带 `assumptionOverrides` 重算 → 同 id 画布刷新 | F5,F8 | P1b | TODO | Mock Contract / Live Contract 分别留证 |
| IT06 | 低置信问题降级为反问 → 带 `clarificationAnswers` 继续执行 | F5,F8 | P1b | TODO | Mock Contract / Live Contract 分别留证 |
| IT07 | 结果「存为卡片」→ 资产库可见 | F7 | P1c | TODO | - |
| IT08 | 结果「钉到看板」→ 看板可见 | F7 | P1c | TODO | - |
| IT09 | 溯源面板展示口径/表/SQL | F6,F8 | P1b | TODO | Mock Contract / Degraded Runtime / Live Contract 分别留证 |
| IT10 | `pnpm typecheck` + `pnpm test` + `pnpm build` 全绿 | 全部 | 全部 | TODO | - |

## 验证环境

- 前端:`dts-copilot-webapp`,`pnpm dev`(端口 3003)
- 验证方式:Playwright E2E + 手工截图存 `../assets/`
- F8 未完成时,IT05/IT06/IT09 不得以真实后台链路标记为 DONE;只能记录 mock 或 degraded 证据。
