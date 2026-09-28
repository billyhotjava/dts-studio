# NV-05: CopilotChat "创建可视化" 按钮

**状态**: READY
**依赖**: NV-03

## 目标

当 Agent 回复包含 SQL 时，在消息下方显示"创建可视化"和"复制 SQL"按钮，点击跳转到 CardEditorPage 自动执行。

## 技术设计

### SQL 提取工具函数

新建 `src/utils/sqlExtractor.ts`：

```typescript
/**
 * 从 markdown 内容中提取 SQL 代码块。
 * 匹配 ```sql ... ``` 或 ``` ... ``` 中以 SELECT/WITH 开头的内容。
 */
export function extractSqlFromMarkdown(content: string): string | null
```

### 消息渲染增强

在 `CopilotChat.tsx` 的 assistant 消息渲染中：

1. 对每条 assistant 消息调用 `extractSqlFromMarkdown(message.content)`
2. 如果提取到 SQL，在消息末尾渲染按钮组：
   - **「SQL 创建可视化」** — 跳转 `/questions/new?sql=${encodeURIComponent(sql)}&db=${datasourceId}&autorun=1`
   - **「复制 SQL」** — `navigator.clipboard.writeText(sql)`
3. 按钮使用项目已有的 `Button` 组件（variant=primary / variant=secondary）

### 跳转 URL 格式

```
/questions/new?sql=SELECT+...&db=3&name=查询结果&autorun=1
```

参数说明：
- `sql`: URL 编码的 SQL 语句
- `db`: 数据库 ID（从 CopilotChat 当前选择的 datasourceId 获取）
- `name`: 可选，从用户提问中截取前 20 字作为默认标题
- `autorun`: 固定为 1，触发自动执行

### 参考

`Nl2SqlEvalPage.tsx` 中已有类似的 SQL 提取和跳转逻辑，可以参考复用。

## 影响文件

- `sqlExtractor.ts`（**新建**）
- `CopilotChat.tsx`（修改：消息渲染增加按钮）

## 完成标准

- [ ] Agent 返回包含 SQL 代码块的消息时，消息下方出现两个按钮
- [ ] 点击"创建可视化"跳转到 CardEditorPage，URL 参数正确
- [ ] 点击"复制 SQL"成功复制到剪贴板
- [ ] 不包含 SQL 的消息不显示按钮
