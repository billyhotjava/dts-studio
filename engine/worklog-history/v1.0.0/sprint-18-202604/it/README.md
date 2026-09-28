# Sprint-18 IT

## 验收维度

| 维度 | 验证方式 |
|------|---------|
| 构建 | `pnpm build` 零警告，target=esnext |
| TypeScript | `pnpm tsc --noEmit` 零错误 |
| Lint | `pnpm lint` 零 error |
| Bundle | 主 chunk < 500KB，echarts 独立 chunk |
| 性能 | Lighthouse Performance > 80 |
| 视觉 | 所有页面视觉无变化（截图对比） |
| 文件大小 | 无文件超 1000 行 |
| CSS | 总行数 < 9,000（从 10,939 减少） |

## 执行命令

```bash
pnpm build          # 构建验证
pnpm tsc --noEmit   # 类型检查
pnpm lint           # Lint 检查
pnpm analyze        # Bundle 分析
pnpm test           # 单元测试
```
