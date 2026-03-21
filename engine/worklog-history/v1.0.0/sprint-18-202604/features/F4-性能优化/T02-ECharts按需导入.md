# T02: ECharts 按需导入

**优先级**: P0
**状态**: READY
**依赖**: F1/T02（bundle 分析确认 echarts 体积）

## 目标

将 echarts 从全量导入改为按需导入，只打包实际使用的图表类型和组件。

## 技术设计

### 当前用法（全量导入）

```typescript
import * as echarts from 'echarts';
// 或
import ReactEChartsCore from 'echarts-for-react';
```

### 改为按需导入

```typescript
import * as echarts from 'echarts/core';
import { LineChart, BarChart, PieChart, GaugeChart, RadarChart, ScatterChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
    LineChart, BarChart, PieChart, GaugeChart, RadarChart, ScatterChart,
    TitleComponent, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent,
    CanvasRenderer,
]);
```

### 预期效果

- 全量 echarts: ~1.2 MB
- 按需导入: ~400-600 KB（取决于使用的组件数）

## 完成标准

- [ ] echarts 按需导入
- [ ] bundle 报告中 echarts chunk 减少 40%+
- [ ] 所有图表类型渲染正常
