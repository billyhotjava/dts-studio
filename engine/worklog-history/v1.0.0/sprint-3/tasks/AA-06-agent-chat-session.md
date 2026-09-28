# AA-06: Agent Chat 会话管理（持久化 + 流式）

**状态**: READY
**依赖**: AA-03

## 目标

实现 Agent Chat 会话管理，支持多轮对话持久化、会话历史查询和流式输出。

## 技术设计

### REST API

```
POST /api/ai/agent/chat/send          — 发送消息（同步）
POST /api/ai/agent/chat/stream        — 发送消息（流式 SSE）
GET  /api/ai/agent/chat/sessions      — 会话列表
GET  /api/ai/agent/chat/{sessionId}   — 会话详情（含消息历史）
DELETE /api/ai/agent/chat/{sessionId} — 删除会话
```

### 数据模型

```sql
CREATE TABLE copilot_ai.ai_chat_session (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(128) NOT NULL,
    title           VARCHAR(256),
    model           VARCHAR(128),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE copilot_ai.ai_chat_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(36) NOT NULL REFERENCES copilot_ai.ai_chat_session(id),
    role            VARCHAR(16) NOT NULL,  -- user/assistant/system/tool
    content         TEXT,
    tool_calls      JSONB,
    tool_call_id    VARCHAR(128),
    created_at      TIMESTAMP DEFAULT NOW()
);
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/chat/AgentChatService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AgentChatResource.java`（新建）
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_004__chat_session.xml`（新建）

## 完成标准

- [ ] 多轮对话正确持久化
- [ ] 流式 SSE 输出包含 Tool 调用中间结果
- [ ] 会话列表和历史查询正常
- [ ] 会话可删除
