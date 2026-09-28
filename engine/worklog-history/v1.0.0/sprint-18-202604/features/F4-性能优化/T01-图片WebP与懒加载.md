# T01: 图片 WebP 优化与懒加载

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

为静态图片资产生成 WebP 版本，所有 `<img>` 标签加 `loading="lazy"`。

## 技术设计

### 构建时图片压缩

```bash
pnpm add -D vite-plugin-image-optimizer
```

```typescript
// vite.config.ts
import { ViteImageOptimizer } from 'vite-plugin-image-optimizer';

plugins: [
    ViteImageOptimizer({
        png: { quality: 80 },
        jpeg: { quality: 80 },
        webp: { quality: 80 },
    }),
],
```

### 代码中加 lazy loading

全局搜索 `<img` 标签，加 `loading="lazy"`：

```tsx
// 改写前
<img src={url} alt={alt} />

// 改写后
<img src={url} alt={alt} loading="lazy" />
```

### 首屏图片例外

首屏可见的关键图片（如 logo）使用 `loading="eager"` 或不设置。

## 完成标准

- [ ] vite-plugin-image-optimizer 安装并配置
- [ ] 所有非首屏 `<img>` 有 `loading="lazy"`
- [ ] 构建后图片体积减少 30%+
