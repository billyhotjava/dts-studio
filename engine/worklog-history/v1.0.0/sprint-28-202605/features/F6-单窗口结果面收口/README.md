# F6: 单窗口结果面收口

**优先级**: P0
**状态**: DONE

## 目标

取消 Agent BI 工作台右侧预览画布,让对话窗口内的 inline SQL 预览成为唯一结果展示面,避免双界面重复执行和重复表格。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 取消右侧 CanvasPanel 并恢复 inline 自动预览 | P0 | DONE | F1,F3 |

## 完成标准

- [x] 工作台 active conversation 不再渲染 `agent-workspace__canvas` / `CanvasPanel`。
- [x] `ConversationThread` 不再接入 `artifactStore`,避免 workbench 模式压制 inline preview。
- [x] 最新 SQL 回答在 `MessageList` 内自动预览,保持单窗口结果面。
- [x] 相关回归测试、typecheck、build 与 webapp 镜像重启验证通过。
