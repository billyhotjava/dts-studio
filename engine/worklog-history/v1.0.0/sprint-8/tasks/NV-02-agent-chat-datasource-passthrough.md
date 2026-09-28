# NV-02: Agent Chat 透传 datasourceId

**状态**: READY
**依赖**: AA-06

## 目标

前端选择的数据源 ID 能透传到 Agent 工具上下文，使 `schema_lookup` 和 `execute_query` 工具查询正确的业务库。

## 技术设计

### Request DTO 变更

`AgentChatRequest`（或等效 DTO）增加可选字段：

```java
@JsonProperty("datasourceId")
Long datasourceId;
```

### 透传路径

```
前端 aiAgentChatSend({ userMessage, datasourceId, sessionId })
    ↓
AgentChatResource.send(request)
    ↓
AgentChatService.processMessage(...)
    ↓
ToolContext(userId, sessionId, datasourceId)  ← 从 request.datasourceId 填入
    ↓
schema_lookup / execute_query 使用 ToolContext.dataSourceId()
```

### 兼容性

- `datasourceId` 为 null 时保持现有行为（使用默认数据源或返回错误）
- `datasourceId` 非 null 时验证该数据源存在且可访问

## 影响文件

- `AgentChatResource.java`（修改：读取 datasourceId）
- Agent 编排 service（修改：构造 ToolContext 时填入 datasourceId）
- 可能需要修改 request DTO 类

## 完成标准

- [ ] curl 调 `/api/ai/agent/chat/send` 带 `datasourceId`，tool 调用日志显示使用了指定数据源
- [ ] 不带 `datasourceId` 时行为不变
