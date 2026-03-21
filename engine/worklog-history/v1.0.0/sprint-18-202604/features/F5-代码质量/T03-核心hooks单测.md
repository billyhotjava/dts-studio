# T03: 核心 hooks 单元测试

**优先级**: P2
**状态**: READY
**依赖**: F3（拆分后更容易测试）

## 目标

为拆分后的核心 hooks 添加 Vitest + React Testing Library 单元测试。

## 技术设计

### 测试框架

```bash
pnpm add -D vitest @testing-library/react @testing-library/jest-dom jsdom
```

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: './src/test/setup.ts',
    },
});
```

### 测试目标

| Hook | 测试点 |
|------|--------|
| useVoiceInput | 浏览器检测、状态流转、超时 |
| usePropertyState | 属性读写、校验 |
| useDragHandlers | 拖拽状态 |
| useCardDataSource | 数据加载、缓存 |

### npm script

```json
"scripts": {
    "test": "vitest run",
    "test:watch": "vitest"
}
```

## 完成标准

- [ ] Vitest + RTL 配置完成
- [ ] 4 个核心 hooks 有单测
- [ ] `pnpm test` 全部通过
