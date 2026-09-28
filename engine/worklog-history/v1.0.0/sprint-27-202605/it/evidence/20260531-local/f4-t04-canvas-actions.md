# F4-T04 CanvasActions 验证

**日期**: 2026-05-31
**范围**: 画布顶部动作行、动作事件契约、禁用/忙碌态、`CanvasPanel` 可选接入。

## 实现结果

- `src/types/artifact.ts` 新增 `CanvasActionType` / `CanvasActionEvent`。
- 新增 `src/components/canvas/CanvasActions.tsx`,渲染 [存为卡片] [钉到看板] [SQL·溯源] [导出] 四个按钮。
- 点击按钮只派发 `{ action, artifact }`,不包含存卡片/看板/导出/溯源的业务实现。
- `CanvasPanel` 支持 `onArtifactAction` / `disabledActions` / `busyAction`,作为 F6/F7 后续接线入口。
- 为避免 `@ant-design/icons` 在当前 React 19 测试别名环境中引入第二份 React,动作行使用本地无依赖小型文字图标。

## 验证命令

```bash
pnpm vitest run src/components/canvas/CanvasActions.test.tsx
```

结果: 1 file / 4 tests passed。

```bash
pnpm vitest run src/components/canvas/ArtifactCanvas.test.tsx src/components/canvas/ArtifactTray.test.tsx src/components/canvas/CanvasPanel.test.tsx src/components/canvas/CanvasActions.test.tsx src/types/artifact.test.ts src/hooks/useArtifactStore.test.tsx
```

结果: 6 files / 22 tests passed。

```bash
pnpm test
```

结果: 43 files / 185 tests passed。

```bash
pnpm typecheck
```

结果: passed。

```bash
pnpm build
```

结果: passed。仍有既有 large chunk warning,本次未引入新的构建失败。

## 说明

F4 只完成画布承载、产物渲染、托盘和事件契约。`trace-sql` 由 F6 实现;`save-card` / `pin-dashboard` / `export` 由 F7 实现。
