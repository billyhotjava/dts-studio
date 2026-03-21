# F1: 构建与目标现代化

**优先级**: P0
**状态**: READY

## 目标

去掉 Chrome 95 兼容负担，构建目标升级到 esnext，安装 bundle 分析工具，清理不需要的 polyfill。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 升级构建目标到 esnext | P0 | READY | - |
| T02 | 安装 bundle 分析器 | P0 | READY | - |
| T03 | 清理 polyfill 和兼容代码 | P0 | READY | T01 |
| T04 | 优化 chunk 分割策略 | P1 | READY | T02 |

## 完成标准

- [ ] browserslist 改为 `Chrome >= 120`
- [ ] vite.config.ts 中 LEGACY_BROWSER_BUILD 逻辑移除
- [ ] esbuild target = esnext
- [ ] tsconfig target = ES2024
- [ ] `@ungap/structured-clone` polyfill 移除
- [ ] `npx vite build` 零警告
- [ ] bundle 分析报告可生成（HTML 可视化）
