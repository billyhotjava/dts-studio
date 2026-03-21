# F4: 性能优化

**优先级**: P1
**状态**: READY

## 目标

优化首屏加载性能和运行时渲染性能。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 图片 WebP 优化与懒加载 | P0 | READY | - |
| T02 | ECharts 按需导入 | P0 | READY | F1/T02 |
| T03 | Ant Design tree-shaking 验证 | P1 | READY | F1/T02 |
| T04 | 路由预加载与 Suspense 优化 | P1 | READY | - |

## 完成标准

- [ ] 首屏 LCP < 2s（Fast 3G 模拟）
- [ ] 图片资产有 WebP 版本
- [ ] echarts 不在首屏 bundle 中
- [ ] 路由切换有 loading fallback
