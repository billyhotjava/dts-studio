# T01: 处理 `Nl2SqlEvalPage` 孤儿页

**优先级**: P2
**状态**: DONE
**依赖**: F1

## 目标

明确 `Nl2SqlEvalPage` 是恢复为治理入口、迁移到测试工具入口,还是删除。

## 技术设计

- `Nl2SqlEvalPage` 无路由、无导航、无运行时引用。
- 删除页面文件,并在 `routes.agentWorkspace.test.ts` 增加孤儿页不存在断言。
- 去留决策记录到 Sprint-28 evidence。

## 影响范围

- `dts-copilot-webapp/src/pages/Nl2SqlEvalPage.tsx`
- `dts-copilot-webapp/src/routes.tsx`
- `dts-copilot-webapp/src/layouts/appNavigation.ts`
- `worklog/v1.0.0/sprint-28-202605/it/evidence/20260531-local/f5-cleanup.md`

## 验证

- [x] `pnpm typecheck`
- [x] `pnpm test -- routes`

## 完成标准

- [x] 页面不再是无路由孤儿。
- [x] 去留决策有证据。
