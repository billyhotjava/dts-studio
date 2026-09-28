# T03: 增加 nav-route 防回归检查

**优先级**: P2
**状态**: DONE
**依赖**: T01,T02

## 目标

防止再次出现导航链接可点击但目标页面不消费 query 或无路由的情况。

## 技术设计

- 增强 `dts-copilot-webapp/src/layouts/appNavigation.test.ts`。
- 覆盖 `PRIMARY_NAV_SECTIONS`、`GOVERNANCE_NAV_ITEMS` 与 `MOBILE_NAV_ITEMS` 中的所有 `to`。
- 对 `/agent-bi?view=...` 这类 query 链接,断言 `AgentWorkspacePage` 的 view allowlist 包含对应值。
- 对普通 path 链接,断言 `routes.tsx` 中存在匹配路由或明确 redirect。

## 影响范围

- `dts-copilot-webapp/src/layouts/appNavigation.test.ts`
- `dts-copilot-webapp/src/pages/AgentWorkspacePage.tsx`
- `dts-copilot-webapp/src/routes.tsx`

## 验证

- [x] `pnpm test -- appNavigation`
- [x] `routes.agentWorkspace.test.ts` 与 `appNavigation.test.ts` 已覆盖普通 route 和 query view 消费端。

## 完成标准

- [x] 导航新增链接必须被测试覆盖。
- [x] query view 不再靠人工记忆接线。
