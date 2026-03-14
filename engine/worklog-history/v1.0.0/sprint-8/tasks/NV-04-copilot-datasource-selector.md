# NV-04: CopilotChat 数据源选择器

**状态**: READY
**依赖**: NV-01, FE-02

## 目标

在 CopilotChat 面板顶部增加数据库下拉选择器，用户可选择查询目标数据源。

## 技术设计

### UI 位置

在 CopilotChat 的 session 选择器下方（或旁边），增加一个数据库下拉：

```
[AI Copilot]
[session selector ▾]  [数据源: 园林业务库 ▾]
─────────────────────
[chat messages...]
```

### 数据加载

- 组件 mount 时调用 `analyticsApi.listDatabases()` 获取数据库列表
- 返回格式：`[{ id, name, engine }]`
- 默认选择第一个数据库

### State 管理

- `selectedDatasourceId: number | null` — 当前选中的数据源 ID
- 存入 `sessionStorage`（`dts-analytics.copilotDatasourceId`）以便页面刷新后保持

### 透传

每次调用 `analyticsApi.aiAgentChatSend()` 时，将 `datasourceId: selectedDatasourceId` 传入请求体。

## 影响文件

- `CopilotChat.tsx`（修改：增加数据源下拉和 state）
- `analyticsApi.ts`（确认 `aiAgentChatSend` 支持 datasourceId 参数）

## 完成标准

- [ ] 下拉列表显示已配置的数据库名称
- [ ] 切换数据源后，新的聊天消息使用新数据源
- [ ] 刷新页面后保持选择
- [ ] 无数据库时显示提示"请先添加数据源"
