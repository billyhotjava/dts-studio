# T02: 拆分 ComponentRenderer.tsx

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

将 ComponentRenderer.tsx (3,868 行) 拆分为 6-8 个文件。

## 技术设计

### 当前结构分析

ComponentRenderer 是大屏运行时的组件渲染引擎，包含：
- 主题解析和颜色映射
- 各种图表类型的 ECharts option 构建
- KPI 卡片、表格、滚动排行等非图表组件
- 拖拽手柄和交互逻辑
- 数据映射和转换

### 拆分方案

```
ComponentRenderer/
├── index.tsx                      # 主入口（组件类型路由）~300 行
├── ComponentRenderer.css          # 样式
├── renderers/
│   ├── ChartRenderer.tsx          # ECharts 图表渲染（line/bar/pie/gauge/radar）~800 行
│   ├── TableRenderer.tsx          # 表格/滚动排行渲染 ~400 行
│   ├── CardRenderer.tsx           # KPI/number-card 渲染 ~300 行
│   ├── TextRenderer.tsx           # markdown-text/title 渲染 ~200 行
│   ├── FilterRenderer.tsx         # filter-select/date-range 渲染 ~300 行
│   └── SpecialRenderer.tsx        # 地图/流线/散点等特殊图表 ~500 行
├── utils/
│   ├── themeResolver.ts           # 主题颜色解析 ~200 行
│   ├── dataMapper.ts              # 数据源到图表数据的映射 ~300 行
│   └── echartOptionsBuilder.ts    # ECharts option 构建器 ~400 行
└── hooks/
    └── useDragHandlers.ts         # 拖拽逻辑 ~200 行
```

## 完成标准

- [ ] 每个文件 < 800 行
- [ ] 大屏运行时渲染正常
- [ ] 所有图表类型渲染无变化
