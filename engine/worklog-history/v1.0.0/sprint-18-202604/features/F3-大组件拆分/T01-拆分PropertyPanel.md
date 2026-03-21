# T01: 拆分 PropertyPanel.tsx

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

将 PropertyPanel.tsx (6,413 行) 拆分为 8-10 个文件，每个 < 800 行。

## 技术设计

### 当前结构分析

PropertyPanel 是大屏编辑器的属性配置面板，包含：
- 各种组件类型的配置表单（line-chart, bar-chart, pie-chart, table 等）
- 数据源配置
- 样式配置
- 交互配置
- 全局变量配置

### 拆分方案

```
PropertyPanel/
├── index.tsx                    # 主入口（路由到各子面板）~200 行
├── PropertyPanel.css            # 样式（保持不变或嵌套改写）
├── panels/
│   ├── DataSourcePanel.tsx      # 数据源配置 ~400 行
│   ├── ChartStylePanel.tsx      # 图表样式配置 ~500 行
│   ├── TextStylePanel.tsx       # 文本样式配置 ~300 行
│   ├── FilterPanel.tsx          # 筛选器配置 ~300 行
│   ├── InteractionPanel.tsx     # 交互配置 ~400 行
│   └── GlobalVarPanel.tsx       # 全局变量配置 ~300 行
├── hooks/
│   ├── usePropertyState.ts      # 属性状态管理 ~200 行
│   └── usePropertyValidation.ts # 属性校验逻辑 ~150 行
└── constants.ts                 # 配置项常量/枚举 ~300 行
```

### 迁移步骤

1. 创建目录结构
2. 提取常量和类型到 `constants.ts`
3. 提取 hooks 到 `hooks/`
4. 逐个提取子面板到 `panels/`
5. 主入口 `index.tsx` 组合子面板
6. 确保导出接口不变（`export { PropertyPanel } from './PropertyPanel'`）

## 完成标准

- [ ] 每个拆分后的文件 < 800 行
- [ ] 大屏编辑器属性面板功能完全正常
- [ ] TypeScript 编译通过
