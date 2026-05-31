# F4-T03 当前产物与产物托盘验证

**日期**: 2026-05-31
**范围**: `CanvasPanel`、`ArtifactTray`、当前产物切换、历史产物托盘、store 单一数据源。

## 实现结果

- 新增 `src/components/canvas/CanvasPanel.tsx`,从注入的 `ArtifactStore` 读取 `current` / `artifacts` / `currentId`,并把当前产物交给 `ArtifactCanvas`。
- 新增 `src/components/canvas/ArtifactTray.tsx`,横向展示历史产物 chip,当前产物高亮,点击切回任一历史产物。
- 托盘 chip 是 `button`,支持 Tab 聚焦和 `ArrowLeft` / `ArrowRight` 快速切换。
- `CanvasPanel` 不持有自己的产物状态,会话产物状态仍只来自 `useArtifactStore`。

## 验证命令

```bash
pnpm vitest run src/components/canvas/ArtifactTray.test.tsx src/components/canvas/CanvasPanel.test.tsx
```

结果: 2 files / 4 tests passed。

```bash
pnpm test
```

结果: 42 files / 181 tests passed。

```bash
pnpm typecheck
```

结果: passed。

```bash
pnpm build
```

结果: passed。仍有既有 large chunk warning,本次未引入新的构建失败。

## 说明

T03 完成画布主区和托盘组装;顶部动作行仍由 F4-T04 实现。
