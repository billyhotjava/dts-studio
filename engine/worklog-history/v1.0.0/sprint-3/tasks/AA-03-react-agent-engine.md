# AA-03: ReAct Agent 引擎抽取

**状态**: READY
**依赖**: AE-04

## 目标

从 dts-platform 抽取 ReAct（Reasoning + Acting）Agent 引擎，支持多轮推理和工具调用。

## 技术设计

### 来源文件

- `dts-platform/service/ai/engine/ReActEngine.java`
- `dts-platform/service/ai/AgentExecutionService.java`
- `dts-platform/domain/ai/AiChatSession.java`
- `dts-platform/domain/ai/AiChatMessage.java`

### ReAct 循环

```
用户消息 → 系统 prompt + 历史 + Tool 定义
    ↓
LLM 推理 → 判断是否需要调用 Tool
    ├─ 需要 → 执行 Tool → 将结果反馈给 LLM → 继续推理
    └─ 不需要 → 返回最终答案
    ↓
最多 10 轮循环
```

### 保留功能

- 多轮推理循环
- Tool 调用与结果反馈
- 会话历史管理
- 待审批动作（pending action approval）
- 流式输出中间结果

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/ReActEngine.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/agent/AgentExecutionService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/AiChatSession.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/AiChatMessage.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/repository/AiChatSessionRepository.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/repository/AiChatMessageRepository.java`（新建）

## 完成标准

- [ ] ReAct 引擎可执行多轮推理（最多 10 轮）
- [ ] Tool 调用和结果反馈正常
- [ ] 会话历史正确持久化
- [ ] 流式输出包含中间推理步骤
