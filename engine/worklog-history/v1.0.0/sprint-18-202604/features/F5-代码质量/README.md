# F5: 代码质量

**优先级**: P2
**状态**: READY

## 目标

清理代码异味，收紧 lint 规则，补充关键组件的单元测试。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 移除 React.FC 类型注解 | P0 | READY | - |
| T02 | Biome 规则收紧 | P1 | READY | - |
| T03 | 核心 hooks 单元测试 | P2 | READY | F3 |

## 完成标准

- [ ] 0 个 React.FC 使用
- [ ] Biome lint 零警告
- [ ] 核心 hooks 有单测覆盖
