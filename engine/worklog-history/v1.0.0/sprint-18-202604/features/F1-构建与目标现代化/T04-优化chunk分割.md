# T04: 优化 chunk 分割策略

**优先级**: P1
**状态**: READY
**依赖**: T02

## 目标

基于 bundle 分析报告，优化 Vite 的 chunk 分割策略，减少首屏加载量。

## 技术设计

### 预期分割策略

```typescript
// vite.config.ts
build: {
    rollupOptions: {
        output: {
            manualChunks: {
                // React 运行时
                'vendor-react': ['react', 'react-dom', 'react-router'],
                // UI 框架
                'vendor-antd': ['antd', '@ant-design/icons'],
                // 图表库（大屏页面才需要）
                'vendor-echarts': ['echarts', 'echarts-for-react', 'echarts-wordcloud'],
                // 大屏数据可视化
                'vendor-dataview': ['@jiaminghi/data-view-react'],
                // 拖拽
                'vendor-dnd': ['react-dnd', 'react-dnd-html5-backend', 'react-grid-layout'],
            },
        },
    },
},
```

### 目标

- 首屏 JS 主 chunk < 300KB (gzip)
- echarts chunk 仅在大屏页面加载
- antd chunk 通过 tree-shaking 只包含使用的组件

## 完成标准

- [ ] manualChunks 配置生效
- [ ] bundle 报告确认分割合理
- [ ] echarts 不在首屏 chunk 中
- [ ] 主 chunk < 500KB（未压缩），< 150KB（gzip）
