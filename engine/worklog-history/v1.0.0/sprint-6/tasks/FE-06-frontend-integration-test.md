# FE-06: 前端集成测试

**状态**: READY
**依赖**: FE-01~05

## 目标

编写前端集成测试，验证核心页面渲染和 API 调用。

## 测试场景

1. **构建验证**: `pnpm build` 无错误，无 TypeScript 类型错误
2. **页面渲染**: 所有核心路由可访问（Hub、仪表盘、查询、屏幕）
3. **AI 聊天**: 聊天面板可打开、可发送消息
4. **iframe 模式**: `?embed=true` 下布局正确
5. **API 调用**: 前端可成功调用 copilot-ai 和 copilot-analytics

## 影响文件

- `dts-copilot-webapp/src/__tests__/routes.test.tsx`（新建）
- `dts-copilot-webapp/src/__tests__/embed.test.tsx`（新建）

## 完成标准

- [ ] `pnpm typecheck` 通过
- [ ] `pnpm build` 无警告
- [ ] 核心路由测试通过
