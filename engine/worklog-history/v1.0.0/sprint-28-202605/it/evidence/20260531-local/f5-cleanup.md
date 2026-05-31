# F5 清理与防回归证据

## 范围

验证 F5-T01~T03 / IT08:

- 处理 `Nl2SqlEvalPage` 孤儿页。
- 清理 `appShellConfig` 中无消费方的 route policy 常量。
- 增加导航目标与工作台 query view 的防回归测试。

## RED

命令:

```bash
cd dts-copilot-webapp
pnpm test -- appNavigation routes.agentWorkspace
node --test tests/appShellConfig.test.ts
```

失败点:

- `routes.agentWorkspace.test.ts` 新增孤儿页断言后失败:`pages/Nl2SqlEvalPage.tsx` 仍存在。
- `appShellConfig.test.ts` 新增死常量断言后失败:`CORE_NAV_PATHS` / `REMOVED_ROUTE_PREFIXES` 仍在源码中导出。

## GREEN

变更:

- 删除 `dts-copilot-webapp/src/pages/Nl2SqlEvalPage.tsx`。
- `dts-copilot-webapp/src/appShellConfig.ts` 仅保留实际被消费的 `APP_HOME_PATH` 与 `APP_HOME_ALIASES`。
- `appNavigation.test.ts` 增加 `PRIMARY_NAV_SECTIONS` / `GOVERNANCE_NAV_ITEMS` / `MOBILE_NAV_ITEMS` 全部 `to` 的检查:
  - 普通路径必须在 `routes.tsx` 存在。
  - `/agent-bi?view=...` 必须被 `normalizeAgentWorkspaceView(...)` allowlist 消费。

命令:

```bash
cd dts-copilot-webapp
pnpm test -- appNavigation routes.agentWorkspace
node --test tests/appShellConfig.test.ts
pnpm typecheck
```

结果:

- `pnpm test -- appNavigation routes.agentWorkspace`: 60 个 test files / 244 个 tests 全部通过。
- `node --test tests/appShellConfig.test.ts`: 2/2 通过。
- `pnpm typecheck`: exit 0。

## 引用检查

命令:

```bash
rg -n "Nl2SqlEvalPage|CORE_NAV_PATHS|REMOVED_ROUTE_PREFIXES" dts-copilot-webapp/src dts-copilot-webapp/tests -S
```

结果:

- 只剩防回归测试中的断言字符串。
- 没有运行时代码继续引用 `Nl2SqlEvalPage`、`CORE_NAV_PATHS`、`REMOVED_ROUTE_PREFIXES`。

## 结论

F5 已完成:

- 无路由孤儿页已删除。
- 死常量已删除。
- 后续新增导航链接时,普通 route 和 `/agent-bi?view=...` 消费端都会被测试覆盖。
