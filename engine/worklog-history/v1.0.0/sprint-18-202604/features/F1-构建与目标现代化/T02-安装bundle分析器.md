# T02: 安装 bundle 分析器

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

安装 rollup-plugin-visualizer，生成 bundle 组成可视化报告。

## 技术设计

```bash
pnpm add -D rollup-plugin-visualizer
```

在 vite.config.ts 中条件启用：

```typescript
import { visualizer } from 'rollup-plugin-visualizer';

plugins: [
    react(),
    process.env.ANALYZE && visualizer({
        open: true,
        filename: 'dist/bundle-report.html',
        gzipSize: true,
        brotliSize: true,
    }),
].filter(Boolean),
```

添加 npm script：

```json
"scripts": {
    "analyze": "ANALYZE=1 vite build"
}
```

## 完成标准

- [ ] `pnpm analyze` 生成 `dist/bundle-report.html`
- [ ] 报告中可看到 echarts、antd、react 等的体积占比
