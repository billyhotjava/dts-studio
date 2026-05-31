# F4-T02 ArtifactCanvas 验证

**日期**: 2026-05-31
**范围**: `ArtifactCanvas` 画布渲染容器、chart/table/report 分发、空态/加载态/错误态。

## 实现结果

- 新增 `src/components/canvas/ArtifactCanvas.tsx`,作为纯展示型组件,只接收 `Artifact | null`,不读取 `AiAgentChatMessage`。
- `type: "chart"` 透传到现有 `ChartRenderer`,复用 echarts 封装和图表回退逻辑。
- `type: "table"` 透传到现有 `DataTable`,复用排序/分页/截断能力。
- `type: "report"` 渲染 AI 报表入口,支持 `spec.reportHref` 跳转。
- 新增 `src/components/canvas/Canvas.css`,补齐画布标题、主体、空态和报表入口样式。

## 验证命令

```bash
pnpm vitest run src/components/canvas/ArtifactCanvas.test.tsx
```

结果: 1 file / 6 tests passed。

```bash
pnpm test
```

结果: 40 files / 177 tests passed。

```bash
pnpm typecheck
```

结果: passed。

```bash
pnpm build
```

结果: passed。仍有既有 large chunk warning,本次未引入新的构建失败。

## 说明

T02 只实现画布渲染容器。当前产物选择、历史产物托盘和动作行分别由 F4-T03/T04 继续实现。
