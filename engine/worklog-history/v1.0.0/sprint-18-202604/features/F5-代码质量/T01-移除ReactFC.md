# T01: 移除 React.FC 类型注解

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

移除 37 处不必要的 `React.FC<>` 类型包装，改为普通函数签名。

## 技术设计

```typescript
// 改写前
const MyComponent: React.FC<Props> = ({ name, value }) => { ... }

// 改写后
function MyComponent({ name, value }: Props) { ... }
// 或
const MyComponent = ({ name, value }: Props) => { ... }
```

### 批量查找

```bash
grep -rn "React\.FC\|React\.FunctionComponent" src/ --include="*.tsx" --include="*.ts"
```

### 为什么移除

- React.FC 隐式包含 `children` prop（React 18 已移除此行为）
- 不提供实际的类型安全增益
- 增加不必要的类型噪音
- TypeScript 官方和 React 团队都不推荐

## 完成标准

- [ ] `grep -r "React.FC" src/` 返回 0 结果
- [ ] TypeScript 编译通过
