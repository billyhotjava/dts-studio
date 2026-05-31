# T02: 清理 `appShellConfig` 死常量

**优先级**: P2
**状态**: DONE
**依赖**: F1,F2

## 目标

处理 `CORE_NAV_PATHS` 和 `REMOVED_ROUTE_PREFIXES` 无消费方的问题,避免误导后续维护。

## 技术设计

- `CORE_NAV_PATHS` / `REMOVED_ROUTE_PREFIXES` 只有测试引用,没有运行时消费方。
- 删除两个导出,保留 `APP_HOME_PATH` 与 `APP_HOME_ALIASES`。
- `tests/appShellConfig.test.ts` 改为断言死常量不再导出。

## 影响范围

- `dts-copilot-webapp/src/appShellConfig.ts`
- `dts-copilot-webapp/src/routes.tsx`
- `dts-copilot-webapp/src/layouts/appNavigation.test.ts`

## 验证

- [x] `pnpm test -- appNavigation`
- [x] `pnpm typecheck`

## 完成标准

- [x] 无未消费导出。
- [x] 保留的配置均有调用方。
