# CS-09: 前端流式 API 方法

**优先级**: P0
**状态**: READY
**依赖**: CS-08

## 目标

在 `analyticsApi.ts` 中新增 `aiAgentChatSendStream()` 函数，使用 `fetch()` + `ReadableStream` 消费 POST-based SSE（不能用 `EventSource`，因为 `EventSource` 只支持 GET）。

## 技术设计

### 类型定义

```typescript
export type StreamEvent =
    | { type: "session"; sessionId: string }
    | { type: "token"; content: string }
    | { type: "tool"; tool: string; status: string }
    | { type: "done"; generatedSql?: string }
    | { type: "error"; error: string };
```

### 函数签名

```typescript
export async function aiAgentChatSendStream(
    body: { sessionId?: string; userMessage: string; datasourceId?: string },
    onEvent: (event: StreamEvent) => void,
): Promise<void>
```

### 实现要点

- `fetch()` POST with `credentials: "include"`, `accept: "text/event-stream"`
- `response.body.getReader()` + `TextDecoder` 逐块读取
- 按 SSE 协议解析 `event:` 和 `data:` 行
- 空行表示一个 SSE 事件结束，触发 `onEvent` 回调
- 处理不完整行（buffer 尾部保留）

## 验收标准

- [ ] TypeScript 类型检查通过
- [ ] 能正确解析 session / token / tool / done / error 五种事件
- [ ] 流中断时 Promise 正常 resolve（不 hang）
