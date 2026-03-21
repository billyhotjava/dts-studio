# Sprint-18: 前端现代化优化 (FO)

**时间**: 2026-04
**前缀**: FO (Frontend Optimization)
**状态**: READY
**目标**: 去掉 Chrome 95 兼容负担，全面采用最新前端技术优化 dts-copilot-webapp 的构建配置、CSS 架构、组件结构和运行时性能。

## 背景

前端审计发现当前技术栈已经很现代（React 19 + Vite 6 + TS 5.9），但存在以下优化空间：

1. **构建目标过旧**：仍保留 Chrome 95 兼容（browserslist + esbuild target），用户实际只用 Chrome/Edge
2. **CSS 没用现代特性**：未使用原生嵌套、Container Queries 等，21 个 CSS 文件 10,939 行
3. **11 个文件超 1000 行**：PropertyPanel.tsx (6413行)、ComponentRenderer.tsx (3868行) 等核心组件臃肿
4. **图片未优化**：1,674 个静态资源无 WebP、无懒加载
5. **无 bundle 分析**：不清楚 echarts/antd 的实际打包体积
6. **37 处 React.FC**：不必要的类型包装

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 说明 |
|----|---------|---------|--------|------|------|
| F1 | 构建与目标现代化 | 4 | P0 | READY | 去 Chrome95、升级 target、bundle 分析 |
| F2 | CSS 现代化 | 4 | P0 | READY | 原生嵌套、Container Queries、清理冗余 |
| F3 | 大组件拆分 | 5 | P1 | READY | PropertyPanel/ComponentRenderer/ScreenHeader 等 |
| F4 | 性能优化 | 4 | P1 | READY | 图片 WebP、echarts 按需、懒加载 |
| F5 | 代码质量 | 3 | P2 | READY | 去 React.FC、补测试、Biome 规则收紧 |

## 完成标准

- [ ] 构建目标 = esnext（Chrome 120+），无 legacy 降级代码
- [ ] CSS 全部改用原生嵌套语法，文件行数减少 20%+
- [ ] 无超过 1000 行的组件文件（拆分后每个 < 800 行）
- [ ] 图片资产支持 WebP，首屏图片有 lazy loading
- [ ] bundle 分析报告生成，主 chunk < 500KB
- [ ] TypeScript 编译零错误
- [ ] Vite 构建零警告
