# F4-T01 产物引用数据模型验证

**日期**: 2026-05-31
**范围**: `Artifact` 类型、消息产物引用契约、`artifactFromMessage` 纯函数、会话级 `useArtifactStore`。

## 实现结果

- 新增 `src/types/artifact.ts`,导出 `Artifact` / `ArtifactSpec` / `ArtifactDataset` / `MessageArtifactRef` / `makeArtifactId` / `artifactFromMessage`。
- 新增 `src/hooks/useArtifactStore.ts`,提供 `artifacts` / `currentId` / `current` / `upsert` / `setCurrent` / `getById` / `clear`。
- `artifactFromMessage` 能把 assistant 消息转成 chart/table/report 三类自包含产物,只保留 `sourceMessageId` 反向引用,不把画布耦合到消息流。
- `useArtifactStore.upsert` 新 id 自动切为当前产物;同 id 原地替换且不新增托盘项;`setCurrent` 只接受已存在 id;`clear` 清空会话级产物状态。

## 验证命令

```bash
pnpm vitest run src/types/artifact.test.ts src/hooks/useArtifactStore.test.tsx
```

结果: 2 files / 8 tests passed。

```bash
pnpm test
```

结果: 39 files / 171 tests passed。

```bash
pnpm typecheck
```

结果: passed。

```bash
pnpm build
```

结果: passed。仍有既有 large chunk warning,本次未引入新的构建失败。

## 说明

T01 只建立模型和 store,不渲染画布、不接入对话流。`ArtifactCanvas` / `ArtifactTray` / `CanvasActions` 分别由 F4-T02/T03/T04 落地。
