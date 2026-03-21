# F2: CSS 现代化

**优先级**: P0
**状态**: READY

## 目标

全面采用 CSS 原生嵌套、Container Queries、`:has()` 选择器等现代 CSS 特性，减少代码量，提升可维护性。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 启用 CSS 原生嵌套 | P0 | READY | - |
| T02 | 核心样式文件嵌套改写 | P0 | READY | T01 |
| T03 | Container Queries 替代 media query | P1 | READY | T01 |
| T04 | 清理冗余 CSS 和死代码 | P1 | READY | T02 |

## 完成标准

- [ ] 所有 CSS 文件使用原生嵌套（无 SCSS/Less 依赖）
- [ ] Copilot 面板使用 Container Queries 替代固定断点
- [ ] CSS 总行数减少 20%+（当前 10,939 行）
- [ ] 无未使用的 CSS 类名
