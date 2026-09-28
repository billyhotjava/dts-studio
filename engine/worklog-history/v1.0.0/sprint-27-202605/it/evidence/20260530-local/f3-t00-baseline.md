# IT00: F3-T00 现状测试基线

**时间**: 2026-05-30
**阶段**: P1a
**状态**: DONE

## 已通过

- `pnpm exec vitest run src/components/copilot/CopilotChat.presentation.test.ts src/components/copilot/copilotSendGuard.test.ts src/components/copilot/copilotStreamReducer.test.ts src/components/copilot/copilotStreamControl.test.ts src/components/copilot/WelcomeCard.test.tsx src/components/copilot/copilotGeneratedReportMessage.test.ts src/pages/screens/hooks/cardDataMapper.test.ts src/pages/screens/renderers/shared/actionUtils.test.ts src/pages/screens/renderers/shared/chartUtils.test.ts`
  - 结果: PASS, 9 个测试文件 / 37 个断言。
- `pnpm typecheck`
  - 结果: PASS。
- `pnpm test`
  - 结果: PASS, 27 个测试文件 / 119 个断言。

## 全量测试现状

- `pnpm test`
  - 结果: PASS。
  - 修复说明:测试期 `react` alias 到 `react-dom` 实际使用的 React,避免 pnpm peer 目录导致 hook dispatcher 不一致;既有 React Testing Library 用例改为等待首屏提交/本地 Probe 组件。

## 结论

F3-T00 的发送守卫、SSE reducer、停止/打断重发、渲染结构锚点已建立可重复运行的全量基线。删除旧路由或拆分 `CopilotChat` 前,必须先跑过 `pnpm typecheck` 与 `pnpm test`。
