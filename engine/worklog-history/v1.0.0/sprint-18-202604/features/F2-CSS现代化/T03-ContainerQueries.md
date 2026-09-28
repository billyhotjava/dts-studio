# T03: Container Queries 替代 media query

**优先级**: P1
**状态**: READY
**依赖**: T01

## 目标

对 Copilot 侧边栏和大屏组件使用 CSS Container Queries，实现基于组件尺寸而非视口尺寸的响应式布局。

## 技术设计

### 场景 1: Copilot 侧边栏

侧边栏宽度可拖拽（240px-720px），内部组件应根据侧边栏实际宽度调整布局，而非视口宽度。

```css
.copilot-sidebar__body {
    container-type: inline-size;
    container-name: copilot;
}

/* 侧边栏宽度 < 360px 时紧凑布局 */
@container copilot (width < 360px) {
    .copilot-chat__db-bar { flex-direction: column; }
    .copilot-chat__msg-actions { flex-wrap: wrap; }
}
```

### 场景 2: 大屏组件

大屏编辑器中组件被缩放到不同尺寸，用 Container Queries 让组件内容自适应：

```css
.screen-component-wrapper {
    container-type: size;
}

@container (width < 200px) {
    .number-card__value { font-size: 1.2rem; }
}

@container (width >= 400px) {
    .number-card__value { font-size: 2.4rem; }
}
```

## 完成标准

- [ ] Copilot 侧边栏拖拽时内部布局自适应
- [ ] 大屏组件在不同尺寸下内容可读
- [ ] 无 JS resize observer hack（纯 CSS 实现）
